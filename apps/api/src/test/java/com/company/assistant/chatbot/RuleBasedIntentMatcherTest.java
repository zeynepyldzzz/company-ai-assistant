package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.company.assistant.directory.DepartmentService;
import com.company.assistant.directory.DirectoryService;
import com.company.assistant.shuttle.ShuttleRoute;
import com.company.assistant.shuttle.ShuttleRouteResponse;
import com.company.assistant.shuttle.ShuttleService;
import com.company.assistant.shuttle.ShuttleStop;
import com.company.assistant.shuttle.ShuttleStopResponse;

/**
 * A-19 (#129): varlik farkindalikli kural katmani. Odak: alan kelimesi + varlik adi
 * birlikteligi, ve alan kelimesi yokken DB'ye hic gidilmemesi.
 */
@ExtendWith(MockitoExtension.class)
class RuleBasedIntentMatcherTest {

    @Mock
    private ShuttleService shuttleService;
    @Mock
    private DirectoryService directoryService;
    @Mock
    private DepartmentService departmentService;

    private RuleBasedIntentMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new RuleBasedIntentMatcher(shuttleService, directoryService, departmentService);
    }

    // --- A-37 (#203): acik tarih + alan kelimesi ---

    /**
     * Kuralin var olma sebebi: embedding SAYININ KENDISINI eslestiriyordu.
     *
     * <p>Olculdu — ayni cumle, yalnizca gun degisiyor: "17 ağustos menü" 0.752 ile geciyor
     * (ornekte "17 temmuz ..." var), "18 ağustos menü" 0.649 ve "19 ağustos menü" 0.660 ile
     * kaciriyordu. Bu test uc gunu birden dogruluyor; kural silinirse ikisi kirilir.
     */
    @Test
    void tarihliMenuSorgusu_gunden_bagimsiz_calisir() {
        for (String gun : List.of("17", "18", "19", "3", "30")) {
            assertThat(matcher.match(gun + " ağustos menü"))
                    .as("gün %s", gun)
                    .isPresent()
                    .get()
                    .extracting(IntentClassificationService.IntentResult::intent)
                    .isEqualTo("yemek_menusu");
        }
    }

    @Test
    void tarihliCalismaDuzeniSorgusu() {
        assertThat(matcher.match("18 ağustos ofiste miyim"))
                .isPresent()
                .get()
                .extracting(IntentClassificationService.IntentResult::intent)
                .isEqualTo("calisma_duzeni");
    }

    @Test
    void sayisalTarihBicimiDeCalisir() {
        assertThat(matcher.match("18.08 menü"))
                .isPresent()
                .get()
                .extracting(IntentClassificationService.IntentResult::intent)
                .isEqualTo("yemek_menusu");
    }

    /**
     * Alan kelimesi yoksa tarih kurali devreye GIRMEZ — tarih tek basina hangi konuyu
     * kastettigini soylemez. O durum takip sorusu mekanizmasina ait (FollowUpDetector),
     * kurala degil.
     *
     * <p>A-38 (#207): bu testin eski notu "burada verifyNoInteractions KULLANILAMAZ" diyordu,
     * cunku "agustos" kelimesi olasi bir calisan adi sanilip DB'ye soruluyordu. Ay adlari
     * artik {@code GENERIC_WORDS}'te; sorgu hic atilmiyor ve garanti sıkılastirildi.
     */
    @Test
    void tarihTekBasina_kuralDevreyeGirmez() {
        assertThat(matcher.match("18 ağustos")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    /** Tarih yoksa menu kelimesi tek basina bu kurali tetiklemez; embedding'e kalir. */
    @Test
    void tarihsizMenuSorgusu_kuralDevreyeGirmez() {
        assertThat(matcher.match("bugün menüde ne var")).isEmpty();
    }

    @Test
    void plakaGuzergahIntentineGider() {
        var result = matcher.match("34 SR 101");

        assertThat(result).isPresent();
        assertThat(result.get().intent()).isEqualTo("servis_guzergah");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] plaka");
    }

    // Olculdu: "kadıköy servisi" ~ ornek "bostancı servisi" -> 0.597. Ozel isim baskin
    // oldugu icin embedding cozemiyordu.
    @Test
    void durakAdiVeServisKelimesiGuzergahIntentineGider() {
        seedShuttle();

        var result = matcher.match("kadıköy servisi");

        assertThat(result).isPresent();
        assertThat(result.get().intent()).isEqualTo("servis_guzergah");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] durak/hat adı");
    }

    @Test
    void saatSorusuVarsaSaatIntentineGider() {
        seedShuttle();

        assertThat(matcher.match("kadıköy servisi kaçta").get().intent()).isEqualTo("servis_saatleri");
    }

    // Varlik yok ama bu uygulamada "servis" + saat sorusu personel servisinden baska bir sey
    // ifade etmiyor. Olculdu: "çarşamba servisi kaçta" 0.671 ile esik altinda kaliyordu.
    @Test
    void varlikYoksaDaServisArtiSaatSaatIntentineGider() {
        seedShuttle();

        var result = matcher.match("çarşamba servisi kaçta");

        assertThat(result.get().intent()).isEqualTo("servis_saatleri");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] servis + saat");
    }

    // A-21 (#146): yakinlik ifadesi servis alaniyla birlesince yonlendirme intent'ine gider.
    @Test
    void enYakinServisSorusuYonlendirmeIntentineGider() {
        var result = matcher.match("bana en yakın servis hangisi");

        assertThat(result.get().intent()).isEqualTo("servis_en_yakin");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] en yakın + servis");
    }

    // KRITIK: bilinen durak adi gecse bile yakinlik kurali ONCE calisir. Aksi halde
    // "kadıköy'e en yakın servis" durak adi eslesmesiyle servis_guzergah'a giderdi ve
    // embedding'e hic dusmezdi — yani ornek cumle eklemek bu vakayi cozmezdi.
    @Test
    void durakAdiGecseDeEnYakinKuraliOnceliklidir() {
        var result = matcher.match("kadıköy'e en yakın servis");

        assertThat(result.get().intent()).isEqualTo("servis_en_yakin");
        verifyNoInteractions(shuttleService);
    }

    // NOBETCI: yakinlik ifadesi TASIMAYAN servis sorulari eski davranisinda kalmali.
    @Test
    void yakinlikIfadesiYoksaServisSorulariDegismez() {
        seedShuttle();

        assertThat(matcher.match("kadıköy servisi kaçta").get().intent()).isEqualTo("servis_saatleri");
        assertThat(matcher.match("kadıköy servisi").get().intent()).isEqualTo("servis_guzergah");
    }

    @Test
    void departmanAdiVeBolumKelimesiDepartmanIntentineGider() {
        seedDepartments();

        var result = matcher.match("muhasebe bölümü");

        assertThat(result.get().intent()).isEqualTo("rehber_departman");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] departman adı");
    }

    // A-25 (#169): olculdu — "Muhasebe çalışanları" 0.644 ile intent_bulunamadi donuyordu.
    @Test
    void departmanAdiVeCalisanKelimesiDepartmanIntentineGider() {
        seedDepartments();

        var result = matcher.match("muhasebe çalışanları");

        assertThat(result.get().intent()).isEqualTo("rehber_departman");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] departman + çalışan listesi");
    }

    @Test
    void departmandaKimlerVarSorusuDaDepartmanIntentineGider() {
        seedDepartments();

        assertThat(matcher.match("muhasebede kimler var").get().intent()).isEqualTo("rehber_departman");
    }

    // KRITIK NOBETCI: durum kelimesi varsa bu kural devreye GIRMEZ. "muhasebede kimler ofiste"
    // bir calisan listesi degil, durum filtreli bir sorudur ve calisma_duzeni'ne aittir.
    @Test
    void durumKelimesiVarsaDepartmanListeKuraliCalismaz() {
        assertThat(matcher.match("muhasebede kimler ofiste")).isEmpty();
        assertThat(matcher.match("bilgi teknolojilerinde kimler uzaktan")).isEmpty();
    }

    // NOBETCI: departman adi olmadan calisan listesi kurali tetiklenmemeli.
    @Test
    void departmanAdiYoksaListeKuraliCalismaz() {
        seedDepartments();

        assertThat(matcher.match("çalışanlar nerede")).isEmpty();
        assertThat(matcher.match("kimler var")).isEmpty();
    }

    @Test
    void calisanAdiVeDahiliKelimesiKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        var result = matcher.match("Ayşe Kaya'nın dahilisi kaç");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] çalışan adı");
    }

    // A-20 (#139): olculdu — "Ayşe Kaya ofiste mi" 0.610 ile intent_bulunamadi donuyordu.
    @Test
    void calisanAdiVeDurumKelimesiKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        var result = matcher.match("Ayşe Kaya ofiste mi");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] çalışan adı + durum");
    }

    @Test
    void calisanAdiVeNeredeSorusuKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        assertThat(matcher.match("Mehmet Demir nerede").get().intent()).isEqualTo("rehber_kisi");
    }

    // Elle test (2026-07-31): "Ayşe Kaya kimdir" 0.565, "ayşe kaya bilgileri" 0.558 ile
    // intent_bulunamadi donuyordu — alan kelimesi yok, embedding ozel isimde kaliyor.
    @Test
    void alanBelirtmeyenKisiSorusuKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        var result = matcher.match("Ayşe Kaya kimdir");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] çalışan adı + bilgi sorusu");
    }

    @Test
    void sirfIsimdenIbaretMesajKisiIntentineGider() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(true);

        var result = matcher.match("ayşe kaya");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] sadece çalışan adı");
    }

    // "sirf isim" kurali alan kelimesi korumasindan yoksun; yerine TUM kelimelerin
    // eslesmesi sart. "Deniz" gercek bir calisan adi ama cumle kisi sorusu degil.
    @Test
    void icindeCalisanAdiGecenSiradanCumleKisiSayilmaz() {
        when(directoryService.existsActiveEmployeeNamed(anyString()))
                .thenAnswer(invocation -> "deniz".equals(invocation.getArgument(0)));

        assertThat(matcher.match("deniz kenarında tatil")).isEmpty();
    }

    // Selamlasma, rehberde benzer bir isim bulunsa bile kisi sorusu sayilmaz.
    @Test
    void selamlasmaSirfIsimKuralinaTakilmaz() {
        assertThat(matcher.match("selam")).isEmpty();
        assertThat(matcher.match("iyi günler")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    // NOBETCI: liste sorulari tek kisilik rehber yanitina KAYMAMALI. Durum kelimesi var
    // ("ofiste") ama ucuncu sahis grup ipucu kurali kapatiyor — DB'ye bile gidilmiyor.
    @Test
    void ucuncuSahisListeSorusuKisiKuralinaGitmez() {
        assertThat(matcher.match("kimler ofiste")).isEmpty();
        assertThat(matcher.match("ofiste olan kişiler")).isEmpty();
        assertThat(matcher.match("çalışanlar nerede")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    // Iki asamali tetikleme: alan kelimesi yoksa servis/departman sorgusu atilmaz.
    //
    // A-20 (#139) NOTU: rehber tarafinda bu garanti artik daraldi — "sirf isim" kurali
    // tanimi geregi alan kelimesi olmadan calismak zorunda, dolayisiyla eslesmeyen her
    // mesajda 1-3 isim sorgusu atiliyor (allMatch ilk eslesmeyende kisa devre yapar,
    // token sayisi de 3 ile sinirli). Bilincli takas: "ayşe kaya" yazan kullanici cevap
    // alamiyordu.
    @Test
    void alanKelimesiYoksaAlanBazliKurallarCalismaz() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(false);

        assertThat(matcher.match("canım sıkıldı")).isEmpty();
        assertThat(matcher.match("bugün yemekte ne var")).isEmpty();

        verifyNoInteractions(shuttleService, departmentService);
    }

    // Alan kelimesi var ama varlik yok -> kural devreye girmez, embedding yoluna kalir.
    @Test
    void alanKelimesiVarVarlikYoksaKuralEslesmez() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(false);

        assertThat(matcher.match("telefon numarası nasıl bulunur")).isEmpty();
    }

    // --- A-38 (#207): jenerik kelimeler ve uc harfli isimler ---

    /**
     * Issue'nun olculmus vakasi: "çarşamba günü uzaktan çalışan kaç kişi var" sorusu
     * {@code rehber_kisi} + "[kural] çalışan adı + durum" donuyordu.
     *
     * <p>Iki kusur ust uste binmisti: (1) "calisan" isim adayi sayiliyor ve soyadi Calisan
     * olan kayda carpiyordu, (2) "kaç kişi" tekil oldugu icin ucuncu sahis guard'i devreye
     * girmiyordu. Kullanici bir SAYI soruyor, tek bir kisinin kartini aliyordu.
     *
     * <p>{@code verifyNoInteractions}: guard artik DB'ye hic gidilmeden kapatiyor.
     */
    @Test
    void jenerikKisiKelimesiIsimAdayiSayilmaz() {
        assertThat(matcher.match("çarşamba günü uzaktan çalışan kaç kişi var")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    // Ay adi artik isim adayi degil: her tarihli mesajda bosuna bir rehber sorgusu atiliyordu.
    // Menu kurali zaten once eslesiyor, ama kural silinse bile isim sorgusu atilmamali.
    @Test
    void ayAdiIcerenMesajIsimSorgusuAtmaz() {
        assertThat(matcher.match("18 ağustos menü").get().intent()).isEqualTo("yemek_menusu");

        verifyNoInteractions(directoryService);
    }

    // Rehberde Can Ozturk ve Yahya Can var; "can" uc harf oldugu icin hicbir zaman
    // bulunamiyordu.
    @Test
    void ucHarfliIsimTekKelimelikMesajdaBulunur() {
        when(directoryService.existsActiveEmployeeNamed("can")).thenReturn(true);

        var result = matcher.match("can");

        assertThat(result.get().intent()).isEqualTo("rehber_kisi");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] sadece çalışan adı");
    }

    // Tek kelimelik mesajda bile eslesme sart; gevseme "her uc harfi kabul et" demek degil.
    @Test
    void tekKelimelikMesajRehberdeYoksaKuralEslesmez() {
        when(directoryService.existsActiveEmployeeNamed(anyString())).thenReturn(false);

        assertThat(matcher.match("abc")).isEmpty();
    }

    /**
     * Alan kelimesi dogrulanmis dallarda sinir 3: "can bey ofiste mi" artik kisi sorusu.
     *
     * <p>Hitaplar ("bey", "abi") aday sayilmamali, yoksa aranan kelime "can" degil "bey"
     * olurdu. {@code DirectoryVariableResolver} bu sorulari bastan beri cevaplayabiliyordu
     * (orada sinir zaten 3 ve "bey"/"hanim" stop-word) — tek engel kural katmaniydi.
     */
    @Test
    void ucHarfliIsimAlanKelimesiVarsaBulunur() {
        when(directoryService.existsActiveEmployeeNamed("can")).thenReturn(true);

        assertThat(matcher.match("can bey ofiste mi").get().matchedPhrase())
                .isEqualTo("[kural] çalışan adı + durum");
        assertThat(matcher.match("can abi ofiste mi").get().intent()).isEqualTo("rehber_kisi");
        assertThat(matcher.match("can kimdir").get().matchedPhrase())
                .isEqualTo("[kural] çalışan adı + bilgi sorusu");
    }

    @Test
    void hitapliUzunIsimDeBulunur() {
        when(directoryService.existsActiveEmployeeNamed("cansu")).thenReturn(true);

        assertThat(matcher.match("cansu abla ofiste mi").get().intent()).isEqualTo("rehber_kisi");
    }

    /**
     * Hitap kelimeleri TEK BASINA kisi sorusu yapmaz — aranan bir isim olmali.
     *
     * <p>Bu test hitap listesinin gercekten calistigini kanitlayan yer: "cansu abla" ornegi
     * kanitlamaz, cunku orada "cansu" ilk token ve {@code anyMatch} kisa devre yapar; "abla"
     * listede olmasa bile sorgulanmazdi. Burada isim hic yok, dolayisiyla eleme calismazsa
     * hitap kelimesi DB'ye gider ve test patlar.
     */
    @Test
    void yalnizcaHitapIcerenMesajKisiSorusuSayilmaz() {
        assertThat(matcher.match("abla ofiste mi")).isEmpty();
        assertThat(matcher.match("hocam nerede")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    /**
     * KRITIK NOBETCI: gevseme alan kelimesine BAGLI. Sinir global olarak 3'e inseydi
     * "canım sıkıldı", "sağol dostum", "ali gel bak" gibi cumlelerde siradan uc harfli
     * kelimeler isim adayi olur ve her mesajda gereksiz rehber sorgusu atilirdi.
     */
    @Test
    void alanKelimesiYoksaUcHarfliKelimeAdaySayilmaz() {
        assertThat(matcher.match("ali gel bak")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    /**
     * KRITIK NOBETCI: birinci sahis durum sorusu REHBER sorusu degildir — kullanicinin kendi
     * plani, {@code calisma_duzeni}'ne ait. Alan kelimesi ("ofiste") var, yani dal aciliyor;
     * kuralı tutan tek sey "ben"/"miyim" kelimelerinin isim adayi sayilmamasi. Aksi halde
     * rehberde adi "Ben..." ile baslayan biri varsa kullanici baskasinin kartini alirdi.
     */
    @Test
    void birinciSahisDurumSorusuKisiKuralinaGitmez() {
        assertThat(matcher.match("ben ofiste miyim")).isEmpty();

        verifyNoInteractions(directoryService);
    }

    // --- A-40 (#209): selamlama kisaltmalari ve "hatlar" ---

    /**
     * Olculdu: "sa" log'da 7 kez, 0.547 ile intent_bulunamadi. Ornek eklemek cozmez —
     * V39'un bulgusu, iki harflik dizgede embedding'in tutunacagi anlamsal sinyal yok.
     *
     * <p>Noktalama temizleniyor: "s.a." Turkce'de yaygin yazim.
     */
    @Test
    void selamlamaKisaltmalariSelamlamayaGider() {
        for (String kisaltma : List.of("sa", "SA", "slm", "mrb", "s.a.", "sa!")) {
            assertThat(matcher.match(kisaltma))
                    .as("kısaltma '%s'", kisaltma)
                    .isPresent()
                    .get()
                    .extracting(IntentClassificationService.IntentResult::intent)
                    .isEqualTo("selamlama");
        }

        verifyNoInteractions(directoryService, shuttleService, departmentService);
    }

    /**
     * KRITIK NOBETCI: kural TAM eslesme yapiyor. Kural katmanindaki diger her sey alt-dize
     * eslesmesiyle calisiyor; bu kural da oyle yazilsaydi "sa" asagidaki kelimelerin
     * hepsinde eslesir ve selamlama intent'ini sessizce calardi.
     */
    @Test
    void icindeKisaltmaGecenKelimelerSelamlamaSayilmaz() {
        assertThat(matcher.match("sabah kaçta geliyor")).isEmpty();
        assertThat(matcher.match("sağol")).isEmpty();
        assertThat(matcher.match("mrb nasılsın")).isEmpty();
    }

    /**
     * "hatlar" eklendi — listede "hatti" vardi, cogulu gozden kacmisti.
     *
     * <p>Bu ekleme tek basina "Hatlar" sorgusunu servis_guzergah'a GONDERMEZ (o liste bir
     * siniflandirici degil, varlik aramasinin kapisi); faydasi varlik adi tasiyan mesajlarda.
     * Test bunu gosteriyor: durak adi + cogul alan kelimesi.
     */
    @Test
    void hatlarKelimesiVarlikAdiylaBirlesinceGuzergahaGider() {
        seedShuttle();

        assertThat(matcher.match("kadıköy hatları").get().intent()).isEqualTo("servis_guzergah");
    }

    // --- A-40 (#209): sirf varlik adindan ibaret mesajlar ---

    // Olculdu: "Muhasebe" 0.605, "Finans" 0.504 ile intent_bulunamadi. Kisi icin
    // isBareEmployeeName vardi, departman karsiligi yoktu.
    @Test
    void sirfDepartmanAdiDepartmanIntentineGider() {
        seedDepartments();

        var result = matcher.match("Muhasebe");

        assertThat(result.get().intent()).isEqualTo("rehber_departman");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] sadece departman adı");
        // Adin ikinci kelimesi de tek basina yeter: "Muhasebe ve Finans".
        assertThat(matcher.match("Finans").get().intent()).isEqualTo("rehber_departman");
    }

    // Olculdu: "kadiköy" 0.449. Departmanla ayni desen, servis tarafinda.
    @Test
    void sirfDurakAdiGuzergahIntentineGider() {
        seedDepartments();
        seedShuttle();

        var result = matcher.match("kadıköy");

        assertThat(result.get().intent()).isEqualTo("servis_guzergah");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] sadece durak/hat adı");
    }

    /**
     * KRITIK NOBETCI: kural TEK KELIMELIK mesajlarla sinirli ve cok kelimelide varlik listesi
     * HIC cekilmez.
     *
     * <p>Cok kelimeliye acilsaydi, sinifin iki asamali tetikleme korumasi varlik sorgulari
     * icin tamamen kalkardi: alan kelimesi tasimayan her kisa mesajda departman + rota +
     * durak sorgusu atilirdi. Bu test yazilirken {@code alanKelimesiYoksaAlanBazliKurallar
     * Calismaz} testi tam olarak bu yuzden kirilmisti — kurali daraltarak duzeltildi.
     *
     * <p>Ayrica "muhasebe ofiste" bir durum sorusudur ve departman KARTINA dusmemeli;
     * kelime sayisi siniri bunu da kapatiyor.
     */
    @Test
    void cokKelimelikMesajdaVarlikListesiCekilmez() {
        assertThat(matcher.match("muhasebe ofiste")).isEmpty();
        assertThat(matcher.match("bugün hava çok güzel")).isEmpty();

        verifyNoInteractions(departmentService, shuttleService);
    }

    // Tek kelimelik DURUM kelimesi varlik adi sayilmaz: nameTokens onu eliyor, sorgu atilmaz.
    @Test
    void tekKelimelikDurumKelimesiVarlikSayilmaz() {
        assertThat(matcher.match("ofiste")).isEmpty();

        verifyNoInteractions(departmentService, shuttleService);
    }

    private void seedShuttle() {
        lenient().when(shuttleService.getAllRoutes())
                .thenReturn(List.of(route(1, "Anadolu Yakasi - Kadikoy Hatti", "34 SR 101")));
        lenient().when(shuttleService.getStopsByRoutes(anyCollection()))
                .thenReturn(Map.of(1, List.of(stop("Kadikoy Iskele", LocalTime.of(7, 0)))));
    }

    private void seedDepartments() {
        when(departmentService.getDepartmentNames())
                .thenReturn(List.of("Muhasebe ve Finans", "Bilgi Teknolojileri"));
    }

    private ShuttleRouteResponse route(Integer id, String name, String plate) {
        ShuttleRoute entity = new ShuttleRoute();
        entity.setId(id);
        entity.setName(name);
        entity.setPlateNumber(plate);
        return new ShuttleRouteResponse(entity);
    }

    private ShuttleStopResponse stop(String name, LocalTime time) {
        ShuttleStop entity = new ShuttleStop();
        entity.setName(name);
        entity.setTime(time);
        entity.setOrderIndex(1);
        return new ShuttleStopResponse(entity);
    }
}
