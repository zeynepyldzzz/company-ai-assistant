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
 * <p>Kural eslesirse embedding hic cagrilmaz. Faz 2'de LLM devreye girdiginde de bu katman
 * onde kalir — yapilandirilmis girdiler icin kural her zaman daha guvenilirdir.
 */
@Component
public class RuleBasedIntentMatcher {

    private static final String INTENT_SHUTTLE_ROUTE = "servis_guzergah";
    private static final String INTENT_SHUTTLE_HOURS = "servis_saatleri";
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
    private static final List<String> PERSON_WORDS =
            List.of("dahili", "telefon", "numara", "mail", "eposta", "e posta");
    private static final List<String> DEPARTMENT_WORDS =
            List.of("bolum", "departman", "birim", "yetkili", "sorumlu");

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
        if (containsAny(text, SHUTTLE_WORDS)) {
            Optional<IntentClassificationService.IntentResult> shuttle = matchShuttle(text);
            if (shuttle.isPresent()) {
                return shuttle;
            }
        }
        if (containsAny(text, DEPARTMENT_WORDS) && matchesDepartment(text)) {
            return rule(INTENT_DEPARTMENT, "departman adı");
        }
        if (containsAny(text, PERSON_WORDS) && matchesEmployee(text)) {
            return rule(INTENT_PERSON, "çalışan adı");
        }
        return Optional.empty();
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
