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

    @Test
    void departmanAdiVeBolumKelimesiDepartmanIntentineGider() {
        seedDepartments();

        var result = matcher.match("muhasebe bölümü");

        assertThat(result.get().intent()).isEqualTo("rehber_departman");
        assertThat(result.get().matchedPhrase()).isEqualTo("[kural] departman adı");
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
