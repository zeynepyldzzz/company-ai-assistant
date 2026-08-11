package com.company.assistant.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.company.assistant.common.DateExpression;
import com.company.assistant.common.TurkishText;
import com.company.assistant.directory.DepartmentService;
import com.company.assistant.directory.DirectoryService;
import com.company.assistant.shuttle.ShuttleRouteResponse;
import com.company.assistant.shuttle.ShuttleService;
import com.company.assistant.shuttle.ShuttleStopResponse;

/**
 * A-17/A-19 (#124, #129): embedding'den ONCE calisan deterministik intent kurallari.
 *
 * <p><b>Gerekce (olculdu, IntentCalibrationIT):</b> kisa sorgularda OZEL ISIM baskindir ve
 * embedding benzerligi cumle kalibi ayni olsa bile dusuk kalir:
 * <pre>
 *   "kadıköy servisi"            ~ ornek "bostancı servisi"            -> 0.597
 *   "Ayşe Kaya'nın dahilisi kaç" ~ ornek "Mehmet Demir'in dahilisi kaç"-> 0.609
 *   "34 SR 101"                  ~ ornek "merhaba"                     -> 0.463
 * </pre>
 * Bu bir esik/ornek sorunu DEGIL: sonsuz sayida durak, calisan ve departman adi var,
 * kullanicinin yazdigi isim hicbir zaman ornektekiyle ayni olmayacak. Varlik tasiyan
 * sorgular yapisal olarak kural katmanina aittir.
 *
 * <p><b>Iki asamali tetikleme:</b> once ALAN KELIMESI aranir (servis/dahili/bolum...),
 * ancak varsa veri tabanina gidilip varlik adi eslestirilir. Bu hem yanlis pozitifi keser
 * ("canım sıkıldı" -> Can Ozturk eslesmesi tetiklenmez, cunku dahili/telefon yok) hem de
 * her mesajda gereksiz sorgu atilmasini onler.
 *
 * <p><b>Tek istisna (A-20/#139):</b> "ayşe kaya" gibi SIRF isimden ibaret mesajlarda alan
 * kelimesi yoktur. Orada koruma tersine cevrilir: anlamli kelimelerin TAMAMI bir calisan
 * adiyla eslesmelidir. Bu daha sert bir kosuldur — "deniz kenarında tatil" cumlesinde
 * "deniz" gercek bir calisan adi olsa bile kural tetiklenmez.
 *
 * <p>Kural eslesirse embedding hic cagrilmaz. Faz 2'de LLM devreye girdiginde de bu katman
 * onde kalir — yapilandirilmis girdiler icin kural her zaman daha guvenilirdir.
 */
@Component
public class RuleBasedIntentMatcher {

    private static final String INTENT_SHUTTLE_ROUTE = "servis_guzergah";
    private static final String INTENT_SHUTTLE_HOURS = "servis_saatleri";
    private static final String INTENT_SHUTTLE_NEAREST = "servis_en_yakin";
    private static final String INTENT_PERSON = "rehber_kisi";
    private static final String INTENT_DEPARTMENT = "rehber_departman";
    private static final String INTENT_MENU = "yemek_menusu";
    private static final String INTENT_SCHEDULE = "calisma_duzeni";

    /**
     * A-37 (#203): ACIK TARIH + alan kelimesi. Bu kalip embedding'e birakilamaz.
     *
     * <p>Olculdu — ayni cumle, yalnizca gun degisiyor:
     * <pre>
     *   "17 ağustos menü"  0.752 GECTI   (ornekte "17 temmuz ..." var)
     *   "18 ağustos menü"  0.649 KACIRDI
     *   "19 ağustos menü"  0.660 KACIRDI
     * </pre>
     *
     * Model ay adini degil SAYININ KENDISINI eslestiriyor: 17 ornekte gectigi icin
     * calisiyor, 18 ve 19 calismiyor. Ornek eklemek bu sorunu cozmez — 31 gun x 12 ay icin
     * ornek yazilamaz. Yapisal kaliplar kural katmanina aittir; plaka, durak adi ve departman
     * adi da ayni sebeple burada.
     */
    private static final List<String> MENU_WORDS =
            List.of("menu", "yemek", "yemekte", "ogle yemegi", "kahvalti");
    private static final List<String> SCHEDULE_WORDS =
            List.of("calisma duzeni", "calisma duzenim", "ofiste", "uzaktan", "izinli", "planim");

    /**
     * Turk plaka bicimi: 2 haneli il kodu + 1-3 harf + 1-5 rakam. Aradaki bosluk opsiyonel,
     * boylece "34 SR 101" ve "34sr101" ayni sekilde yakalanir. Kelime siniri sart.
     */
    private static final Pattern PLATE = Pattern.compile("\\b\\d{2}\\s?[a-z]{1,3}\\s?\\d{1,5}\\b");

    // --- Alan kelimeleri: kural yalnizca bunlardan biri gecerse devreye girer ---
    // "hat" BILEREK yok: alt-dize olarak "hata", "hatta", "rahat" gibi kelimeleri yakalar ve
    // her birinde bosuna guzergah/durak sorgusu atardi. "hatti" yeterince ayirt edici.
    private static final List<String> SHUTTLE_WORDS =
            List.of("servis", "guzergah", "durak", "hatti");
    private static final List<String> SHUTTLE_TIME_WORDS = List.of("saat", "kacta", "kalkis", "kalkiyor");

    /**
     * A-21 (#146): yakinlik ifadeleri. Bu kural SHUTTLE bloğundan ONCE calismali, cunku
     * "kadıköy'e en yakın servis" cumlesinde bilinen bir durak adi geciyor ve mevcut kural
     * onu dogrudan servis_guzergah'a yonlendirirdi — embedding'e hic dusmeden. Yani yalnizca
     * ornek cumle eklemek bu vakayi COZMEZDI.
     */
    private static final List<String> NEAREST_WORDS = List.of("en yakin", "yakinimdaki", "yakinimda");
    // "posta" A-20'de eklendi: "e-posta" ASCII katlamadan sonra tireli kalir ve ne "eposta"
    // ne de "e posta" alt-dizesini icerir — en yaygin yazim bicimi kurala hic takilmiyordu.
    private static final List<String> PERSON_WORDS =
            List.of("dahili", "telefon", "numara", "mail", "posta", "eposta");

    /**
     * A-20 (#139): kisi hakkinda DURUM sorulari. "Ayşe Kaya ofiste mi" 0.610 ile
     * intent_bulunamadi donuyordu — cumle kalibi rehber ornekleriyle ayni olsa bile ozel
     * isim benzerligi asagi cekiyor.
     *
     * <p>PERSON_WORDS'ten AYRI liste: bu kelimeler tek baslarina liste sorusu da olabilir
     * ("kimler ofiste"), o yuzden ucuncu sahis grup ipucu varken kural devreye girmemeli.
     */
    // "ofisde" yaygin bir yazim varyanti (elle test, 2026-08-03). Burada KOK ("ofis")
    // kullanilamaz: bu liste asagida nameTokens() icinde isim adaylarini elemek icin de
    // kullaniliyor ve orada TAM kelime eslesmesi yapiliyor — kok koyulursa "ofiste" tokeni
    // elenmez ve her mesajda gereksiz rehber sorgusu atilir.
    private static final List<String> PERSON_STATUS_WORDS =
            List.of("ofiste", "ofisde", "uzaktan", "izinde", "izinli", "nerede", "evden");

    /**
     * Alan belirtmeyen kisi sorulari: "Ayşe Kaya kimdir", "Ayşe Kaya'nın bilgileri".
     * Olculdu (elle test, 2026-07-31): 0.565 / 0.558 ile intent_bulunamadi donuyordu.
     * Bunlar da tekil kisi sorusudur; DirectoryVariableResolver alan tespit edemeyip
     * TAM KART basar — istenen davranis zaten budur.
     */
    private static final List<String> PERSON_INFO_WORDS =
            List.of("kimdir", "bilgileri", "iletisim", "hakkinda");

    /** "ayşe kaya" gibi sirf isimden ibaret mesajlarda kontrol edilecek en fazla kelime. */
    private static final int BARE_NAME_MAX_TOKENS = 3;

    /**
     * Selamlasma/nezaket kelimeleri isim adayi SAYILMAZ. Aksi halde rehbere "Selami" adinda
     * biri eklendigi gun "selam" mesaji LIKE ile eslesip selamlama intent'ini calardi —
     * kural embedding'den once calistigi icin sessizce ve aciklamasiz.
     */
    private static final List<String> SMALL_TALK_WORDS = List.of(
            "selam", "selamlar", "merhaba", "gunaydin", "iyi", "gunler", "aksamlar",
            "tesekkur", "tesekkurler", "sagol", "sagolun", "kolay", "gelsin");
    private static final List<String> DEPARTMENT_WORDS =
            List.of("bolum", "departman", "birim", "yetkili", "sorumlu");

    /**
     * A-25 (#169): "muhasebe çalışanları" tipi sorular — departmanin CALISAN LISTESI.
     * Olculdu: "Muhasebe çalışanları" 0.644, "Muhasebede kimler var" 0.590, "Bilgi
     * teknolojileri çalışanları" 0.613 ile intent_bulunamadi donuyordu ve en yakin cumleler
     * IKI AYRI kategoriye dagiliyordu — yani ortada boyle bir kalip yoktu.
     *
     * <p>DEPARTMENT_WORDS'ten ayri liste, cunku bu kelimeler DURUM sorulariyla birlesince
     * baska bir seye donusuyor: "muhasebede kimler ofiste" bir calisan listesi degil, durum
     * filtreli bir sorudur ve calisma_duzeni'ne aittir. Ayrim asagida durum kelimesi
     * kontroluyle yapiliyor.
     */
    private static final List<String> DEPARTMENT_ROSTER_WORDS =
            List.of("calisan", "kimler", "ekip", "personel");

    /**
     * Varlik adlarindan anahtar kelime turetirken ve isim adayi ararken elenen, ayirt edici
     * olmayan kelimeler.
     *
     * <p>A-38 (#207): jenerik KISI kelimeleri eklendi. Olculdu — "çarşamba günü uzaktan
     * çalışan kaç kişi var" sorusu {@code rehber_kisi}'ye gidiyordu: "calisan" isim adayi
     * sayiliyor ve A-34'ten beri arama kelime basi eslesmesi yaptigi icin soyadi Calisan olan
     * kayda ({@code LIKE 'calisan%'}) carpiyordu. Kullanici bir SAYI soruyor, tek bir kisinin
     * kartini aliyordu.
     *
     * <p>Ay adlari {@link DateExpression#monthNames()}'den geliyor, kopyalanmiyor.
     *
     * <p>Bedeli bilinerek kabul edildi: soyadi "Calisan" olan biri artik YALNIZCA soyadiyla
     * bulunamaz (adiyla bulunur). Jenerik bir kelimenin her mesajda yanlis tetiklemesi, nadir
     * bir soyadin kaybindan pahali.
     */
    private static final List<String> GENERIC_WORDS = Stream.concat(
            Stream.of("servis", "servisi", "hat", "hatti", "yakasi", "iskele", "durak", "duragi",
                    "departman", "departmani", "birim", "bilgi", "bilgisi",
                    "calisan", "calisanlar", "kisi", "kisiler", "personel"),
            DateExpression.monthNames().stream()).toList();

    /**
     * Alan kelimesi OLMAYAN dalda ({@link #isBareEmployeeName}) isim adayi siniri.
     *
     * <p>Global olarak 3'e indirilemez: "canım sıkıldı", "sağol dostum" gibi cumlelerde
     * siradan uc harfli kelimeler isim adayi olur ve her mesajda gereksiz rehber sorgusu
     * atilir. Orada kuralı tutan tek sey uzunluk.
     */
    private static final int MIN_TOKEN_LENGTH = 4;

    /**
     * A-38 (#207): uc harfli isimler icin gevsetilmis sinir. Rehberde Can Ozturk ve Yahya Can
     * var ama "can" uc harf oldugu icin aday listesinden eleniyordu — bu isim hicbir zaman
     * bulunamiyordu.
     *
     * <p>Iki yerde gecerli, ikisinde de baska bir koruma zaten devrede:
     * <ul>
     *   <li>Alan kelimesi DOGRULANMIS dallar ({@link #matchesEmployee}): iki asamali
     *       tetikleme sarti saglanmis demektir. "can bey ofiste mi" bu sayede calisir;
     *       "canım sıkıldı" cumlesinde alan kelimesi olmadigi icin dal hic acilmaz.</li>
     *   <li>TEK KELIMELIK mesajlar: orada zaten baska yorum yok, kullanici ya bir isim ya bir
     *       anahtar kelime yazmistir.</li>
     * </ul>
     */
    private static final int MIN_SHORT_NAME_LENGTH = 3;

    /**
     * A-38 (#207): sinir 3'e inince aday olmaya baslayan, isim OLMAYAN kelimeler.
     *
     * <p>{@code DirectoryVariableResolver.STOP_WORDS}'un kural katmanindaki aynasi — orada
     * "bey"/"hanim"/"bana"/"miyim" zaten eleniyordu, cunku o sinifin siniri bastan beri 3'tu.
     * Iki katman ayni kelimeleri elemezse kural, resolver'in reddedecegi bir ismi eslestirir.
     *
     * <p>Hitaplar ("bey", "abi") KRITIK: "can bey ofiste mi" sorusunda aranmasi gereken tek
     * kelime "can". Birinci sahis kaliplari da burada — "ben ofiste miyim" bir REHBER sorusu
     * degil, kullanicinin kendi plani; kural devreye girerse baskasinin kartina kayar.
     */
    private static final List<String> NON_NAME_WORDS = List.of(
            // Hitaplar. Cekimli/seslenme bicimleri de burada: Turkce sondan eklemeli ve
            // eleme TAM kelime eslesmesiyle calisiyor, yani "abi" yazmak "abim"i elemez.
            "bey", "beyin", "beyefendi", "hanim", "hanimin", "hanimefendi",
            "abi", "abim", "abicim", "agabey", "abla", "ablam", "ablacim",
            "kardes", "kardesim", "hoca", "hocam", "amca", "teyze",
            // Birinci/ikinci sahis: soru rehbere degil kullanicinin kendisine ait.
            "ben", "sen", "biz", "siz", "bana", "beni", "benim", "miyim", "miydim", "misin",
            "bir", "var", "yok", "kac", "gun", "ama", "ise", "nin", "nun", "den", "dan");

    /** Her isim adayi bir DB sorgusu; ust sinir performans icin. */
    private static final int MAX_NAME_TOKENS = 5;

    private final ShuttleService shuttleService;
    private final DirectoryService directoryService;
    private final DepartmentService departmentService;

    public RuleBasedIntentMatcher(ShuttleService shuttleService,
                                  DirectoryService directoryService,
                                  DepartmentService departmentService) {
        this.shuttleService = shuttleService;
        this.directoryService = directoryService;
        this.departmentService = departmentService;
    }

    /**
     * A-38 (#207): isim adayi olamayacak kelimelerin tamami. Disariya YALNIZCA bekci test
     * icin aciliyor (RuleBasedIntentMatcherNameCollisionIntegrationTest).
     *
     * <p>Gerekce: rehbere bu kelimelerden biriyle AYNI adda biri eklendigi gun o kisi kural
     * katmaninda bulunamaz hale gelir — sessizce, hicbir hata vermeden. Listeyi teste
     * kopyalamak bu riski kapatmaz, cunku kopya ile asil liste zamanla ayrisir; tek kaynak
     * okunmali.
     */
    static List<String> reservedNameWords() {
        return Stream.concat(GENERIC_WORDS.stream(), NON_NAME_WORDS.stream()).distinct().toList();
    }

    public Optional<IntentClassificationService.IntentResult> match(String message) {
        String text = TurkishText.foldToAscii(message);

        if (PLATE.matcher(text).find()) {
            return rule(INTENT_SHUTTLE_ROUTE, "plaka");
        }
        // A-37 (#203): acik tarih + alan kelimesi. Menu kontrolu once: "17 agustos menu"
        // ifadesinde alan kelimesi tek basina ayirt edici, calisma duzeni kelimeleriyle
        // cakismiyor. Iki alan kelimesi birden gecerse ("17 agustos ofiste yemek") menu
        // kazanir — nadir ve zararsiz.
        if (DateExpression.mentionsDate(text)) {
            if (containsAny(text, MENU_WORDS)) {
                return rule(INTENT_MENU, "tarih + menü");
            }
            if (containsAny(text, SCHEDULE_WORDS)) {
                return rule(INTENT_SCHEDULE, "tarih + çalışma düzeni");
            }
        }
        // A-21: yakinlik + servis alani -> yonlendirme intent'i. Durak adi kuralindan ONCE.
        if (containsAny(text, NEAREST_WORDS) && containsAny(text, SHUTTLE_WORDS)) {
            return rule(INTENT_SHUTTLE_NEAREST, "en yakın + servis");
        }
        if (containsAny(text, SHUTTLE_WORDS)) {
            Optional<IntentClassificationService.IntentResult> shuttle = matchShuttle(text);
            if (shuttle.isPresent()) {
                return shuttle;
            }
        }
        if (containsAny(text, DEPARTMENT_WORDS) && matchesDepartment(text)) {
            return rule(INTENT_DEPARTMENT, "departman adı");
        }
        // A-25: departman adi + calisan listesi istegi. DURUM kelimesi varsa devreye GIRMEZ —
        // "muhasebede kimler ofiste" durum filtreli bir sorudur ve calisma_duzeni'ne aittir.
        if (containsAny(text, DEPARTMENT_ROSTER_WORDS)
                && !containsAny(text, PERSON_STATUS_WORDS)
                && matchesDepartment(text)) {
            return rule(INTENT_DEPARTMENT, "departman + çalışan listesi");
        }
        if (containsAny(text, PERSON_WORDS) && matchesEmployee(text)) {
            return rule(INTENT_PERSON, "çalışan adı");
        }
        // Asagidaki uc dal yalnizca TEKIL kisi sorulari icindir: "kimler ofiste" bir liste
        // sorusudur ve calisma_duzeni'nde kalmalidir (A-14 rehber kaynakli yanit). Guard
        // disarida duruyor ki hicbir dal onu atlamasin — ve liste sorularinda DB'ye hic
        // gidilmesin.
        if (!TurkishText.mentionsThirdPersonGroup(text)) {
            if (containsAny(text, PERSON_STATUS_WORDS) && matchesEmployee(text)) {
                return rule(INTENT_PERSON, "çalışan adı + durum");
            }
            if (containsAny(text, PERSON_INFO_WORDS) && matchesEmployee(text)) {
                return rule(INTENT_PERSON, "çalışan adı + bilgi sorusu");
            }
            if (isBareEmployeeName(text)) {
                return rule(INTENT_PERSON, "sadece çalışan adı");
            }
        }
        return Optional.empty();
    }

    /**
     * Mesaj SIRF isimden ibaret mi ("ayşe kaya"). Alan kelimesi yoktur, o yuzden iki asamali
     * tetiklemenin normal korumasi calismaz — yerine daha sert bir kosul konur: anlamli
     * kelimelerin TAMAMI bir calisan adiyla eslesmeli.
     *
     * <p>Bu, "deniz kenarında tatil" gibi cumleleri disarida tutar (ilk eslesmeyen kelimede
     * kisa devre olur, "deniz" gercek bir calisan adi olsa bile). Kelime siniri da var:
     * uzun cumlelerde zaten alan kelimesi bulunur, orada bu dala ihtiyac yok.
     */
    private boolean isBareEmployeeName(String text) {
        // Tek kelimelik mesajda alan kelimesi OLAMAZ, dolayisiyla iki asamali tetiklemenin
        // korumasi da yok; yerine "baska yorum yok" gercegi geciyor ve sinir orada gevser.
        int minLength = words(text).size() == 1 ? MIN_SHORT_NAME_LENGTH : MIN_TOKEN_LENGTH;
        List<String> tokens = nameTokens(text, minLength);
        return !tokens.isEmpty()
                && tokens.size() <= BARE_NAME_MAX_TOKENS
                && tokens.stream().allMatch(directoryService::existsActiveEmployeeNamed);
    }

    /**
     * Servis alani. Bilinen durak/hat adi gecerse kesin; gecmese bile "servis" + saat sorusu
     * ("çarşamba servisi kaçta") bu uygulamada personel servisinden baska bir sey ifade etmez.
     */
    private Optional<IntentClassificationService.IntentResult> matchShuttle(String text) {
        boolean asksTime = containsAny(text, SHUTTLE_TIME_WORDS);
        if (mentionsShuttleEntity(text)) {
            return rule(asksTime ? INTENT_SHUTTLE_HOURS : INTENT_SHUTTLE_ROUTE, "durak/hat adı");
        }
        if (text.contains("servis") && asksTime) {
            return rule(INTENT_SHUTTLE_HOURS, "servis + saat");
        }
        return Optional.empty();
    }

    private boolean mentionsShuttleEntity(String text) {
        List<ShuttleRouteResponse> routes = shuttleService.getAllRoutes();
        if (routes.isEmpty()) {
            return false;
        }
        boolean routeNameHit = routes.stream()
                .anyMatch(route -> mentionsName(text, route.getName()));
        if (routeNameHit) {
            return true;
        }
        return shuttleService.getStopsByRoutes(routes.stream().map(ShuttleRouteResponse::getId).toList())
                .values().stream()
                .flatMap(List::stream)
                .map(ShuttleStopResponse::getName)
                .anyMatch(name -> mentionsName(text, name));
    }

    /**
     * Yalnizca ADLAR cekilir. searchDepartments() yonetici bilgisini de kurdugu icin lazy
     * proxy acar ve HTTP istegi disinda (IT, zamanlanmis is) LazyInitializationException
     * verir — kural katmani her baglamdan cagrilabilmeli.
     */
    private boolean matchesDepartment(String text) {
        return departmentService.getDepartmentNames().stream()
                .anyMatch(name -> mentionsName(text, name));
    }

    /**
     * Calisan adi eslestirmesi mevcut rehber sorgusuyla yapilir (LIKE), tum tabloyu cekmeden.
     * Alan kelimesi zaten dogrulandigi icin "can"/"deniz" gibi siradan kelimelerin yanlis
     * tetiklemesi pratikte kalmaz.
     */
    private boolean matchesEmployee(String text) {
        // A-38 (#207): sinir burada 3 — bu metoda YALNIZCA alan kelimesi dogrulandiktan sonra
        // giriliyor (dahili/telefon/ofiste/kimdir...), yani iki asamali tetiklemenin korumasi
        // zaten devrede. "can bey ofiste mi" bu sayede calisiyor; alan kelimesi tasimayan
        // "canım sıkıldı" cumlesinde bu dal hic acilmadigi icin risk olusmuyor.
        return nameTokens(text, MIN_SHORT_NAME_LENGTH).stream()
                .anyMatch(directoryService::existsActiveEmployeeNamed);
    }

    /**
     * Isim adayi kelimeler. Ust sinir var cunku her aday bir DB sorgusu demek; 3 cok dardi
     * ("lütfen bana Ayşe Kaya'nın telefonunu ver" gibi cumlede isim 4. sirada kalabiliyor).
     */
    /**
     * Mesajin anlamli kelimeleri. Bos parcalar atilir: bastaki/sondaki noktalama
     * {@code split()}'te bos token uretir ve "can?" mesajini tek kelimelik saymaktan
     * alikoyardi.
     */
    private List<String> words(String text) {
        return List.of(text.split("[^a-z0-9]+")).stream()
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private List<String> nameTokens(String text, int minLength) {
        return words(text).stream()
                .filter(token -> token.length() >= minLength)
                .filter(token -> !GENERIC_WORDS.contains(token))
                .filter(token -> !NON_NAME_WORDS.contains(token))
                .filter(token -> !PERSON_WORDS.contains(token))
                .filter(token -> !PERSON_STATUS_WORDS.contains(token))
                .filter(token -> !PERSON_INFO_WORDS.contains(token))
                .filter(token -> !SMALL_TALK_WORDS.contains(token))
                .distinct()
                .limit(MAX_NAME_TOKENS)
                .toList();
    }

    private boolean mentionsName(String text, String name) {
        if (name == null) {
            return false;
        }
        return List.of(TurkishText.foldToAscii(name).split("[^a-z0-9]+")).stream()
                .filter(word -> word.length() >= MIN_TOKEN_LENGTH)
                .filter(word -> !GENERIC_WORDS.contains(word))
                .anyMatch(text::contains);
    }

    private boolean containsAny(String text, List<String> words) {
        return words.stream().anyMatch(text::contains);
    }

    /** Kural eslesmeleri chat_message_log'da "[kural] ..." etiketiyle ayirt edilir. */
    private Optional<IntentClassificationService.IntentResult> rule(String intent, String label) {
        return Optional.of(new IntentClassificationService.IntentResult(
                intent, 1.0, "[kural] " + label, true));
    }
}
