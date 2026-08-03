package com.company.assistant.chatbot;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

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

    /** Varlik adlarindan anahtar kelime turetirken elenen, ayirt edici olmayan kelimeler. */
    private static final List<String> GENERIC_WORDS = List.of(
            "servis", "servisi", "hat", "hatti", "yakasi", "iskele", "durak", "duragi",
            "departman", "departmani", "birim", "bilgi", "bilgisi");

    private static final int MIN_TOKEN_LENGTH = 4;
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

    public Optional<IntentClassificationService.IntentResult> match(String message) {
        String text = TurkishText.foldToAscii(message);

        if (PLATE.matcher(text).find()) {
            return rule(INTENT_SHUTTLE_ROUTE, "plaka");
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
        List<String> tokens = nameTokens(text);
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
        return nameTokens(text).stream().anyMatch(directoryService::existsActiveEmployeeNamed);
    }

    /**
     * Isim adayi kelimeler. Ust sinir var cunku her aday bir DB sorgusu demek; 3 cok dardi
     * ("lütfen bana Ayşe Kaya'nın telefonunu ver" gibi cumlede isim 4. sirada kalabiliyor).
     */
    private List<String> nameTokens(String text) {
        return List.of(text.split("[^a-z0-9]+")).stream()
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .filter(token -> !GENERIC_WORDS.contains(token))
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
