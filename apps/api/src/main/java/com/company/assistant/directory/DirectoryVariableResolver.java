package com.company.assistant.directory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.company.assistant.common.TurkishText;
import com.company.assistant.schedule.StatusDayResolver;

/**
 * A-15 (#117): 'rehber_kisi' intent'i icin kisi arama yaniti uretir.
 *
 * Uretilen anahtar V26 template'iyle eslesir:
 *   {{kisi_bilgisi}} -> tek kisinin rehber bilgileri, netlestirme sorusu veya bulunamadi metni
 *
 * Isim eslestirmede A-12'deki "veriden anahtar kelime turet" yontemi ham haliyle YETMEZ:
 * aktif calisanlar arasinda "Can Ozturk" ve "Deniz Celik" var; "can" ve "deniz" siradan Turkce
 * kelimeler. Iki katman korur:
 *   1. Intent kapisi — bu resolver yalnizca rehber_kisi intent'inde calisir. "canim sikildi"
 *      o intent'e siniflanmaz, isim eslestirme hic devreye girmez.
 *   2. Skor — adayin ad kelimelerinden kaci mesajda geciyor sayilir; en yuksek skoru birden
 *      fazla kisi paylasiyorsa RASTGELE secim yerine netlestirme sorusu doner.
 *
 * Donen alanlar rehber ekraninin gosterdikleriyle sinirli. EmployeeResponse rol bilgisi de
 * tasiyor (C-11/#85) ama chatbot basmaz.
 */
@Component
public class DirectoryVariableResolver {

    private static final String DIRECTORY_INTENT = "rehber_kisi";
    private static final String VARIABLE = "kisi_bilgisi";

    private static final String NO_NAME =
            "Kimi aradığını yazarsan (ad veya soyad) rehberdeki bilgilerini getirebilirim.";
    private static final String NOT_FOUND =
            "Aradığın kişiyi rehberde bulamadım. Şirket Rehberi bölümünden isimle arama yapabilirsin.";
    private static final String UNKNOWN_FIELD = "belirtilmemiş";

    /** Isim adayi olmayan kelimeler; sorgu gurultusunu ve gereksiz DB cagrisini onler. */
    private static final Set<String> STOP_WORDS = Set.of(
            "dahili", "dahilisi", "dahilisini", "telefon", "telefonu", "numara", "numarasi",
            "eposta", "email", "mail", "adresi", "bilgisi", "bilgileri", "iletisim",
            "departman", "departmani", "departmaninda", "hangi", "kimin", "kimdir",
            "nerede", "nedir", "kacti", "ofiste", "calisiyor", "bulabilir", "miyim",
            "bey", "beyin", "hanim", "hanimin", "bana", "lutfen", "acaba",
            // A-20 (#139): kural katmani artik "X ofiste mi / X uzaktan mi" sorularini da
            // buraya yonlendiriyor; durum kelimeleri isim adayi sayilmamali.
            "uzaktan", "izinde", "izinli", "evden", "durumu", "calisma");

    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MAX_CANDIDATES = 25;

    /**
     * A-20 (#139): sorunun ODAGINDAKI alan. Tespit edilemezse tam kart basilir — "Ayşe Kaya
     * kimdir" gibi acik uclu sorularda dogru davranis budur.
     *
     * <p>Gerekce: "Ayşe Kaya ofiste mi" sorusuna dort satirlik kart donmek, kullaniciyi
     * kendi sorusunun cevabini kartin icinde aramaya zorluyor. Sorulan alan biliniyorsa
     * cevap tek satir olmali.
     */
    private enum Field {
        PHONE("telefon"),
        EMAIL("e-posta"),
        DEPARTMENT("departman"),
        OFFICE_STATUS("ofis durumu");

        private final String label;

        Field(String label) {
            this.label = label;
        }
    }

    private static final List<String> PHONE_CUES = List.of("dahili", "telefon", "numara");
    // "posta" secildi cunku "e-posta" ASCII katlamadan sonra da tireli kalir ve "eposta"
    // alt-dizesini ICERMEZ; "mail" ayrica "email"i de yakalar.
    private static final List<String> EMAIL_CUES = List.of("mail", "posta");
    private static final List<String> DEPARTMENT_CUES =
            List.of("departman", "bolum", "birim", "nerede calisiyor", "hangi ekip");
    private static final List<String> STATUS_CUES =
            List.of("ofiste", "uzaktan", "izinde", "izinli", "evden", "durumu", "nerede");

    private final DirectoryService directoryService;
    private final StatusDayResolver statusDayResolver;

    public DirectoryVariableResolver(DirectoryService directoryService,
                                     StatusDayResolver statusDayResolver) {
        this.directoryService = directoryService;
        this.statusDayResolver = statusDayResolver;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!DIRECTORY_INTENT.equals(intentName)) {
            return Map.of();
        }

        String foldedText = TurkishText.foldToAscii(message);
        List<String> tokens = nameTokens(foldedText);
        if (tokens.isEmpty()) {
            return Map.of(VARIABLE, NO_NAME);
        }

        // A-40 (#209): gun cikarimi. Onceden hic yapilmiyordu — "ayşe kaya çarşamba ofiste mi"
        // sorusuna BUGUNUN durumu donuyordu, ustelik "şu an" diyerek. Tek satirlik, kendinden
        // emin bir cevap; kullanicinin supheye dusmesi icin hicbir isaret yoktu.
        //
        // Cikarim YALNIZCA durum sorusunda yapiliyor: telefon/e-posta sorusuna uygulansaydi
        // "ayşe kaya ağustos telefonu" gibi bir mesajda cozulemeyen tarih yuzunden telefon
        // cevabi bloklanirdi — oysa soru gunle ilgili degil.
        Field field = detectField(foldedText);
        StatusDayResolver.DayScope day = field == Field.OFFICE_STATUS
                ? statusDayResolver.resolve(foldedText)
                : StatusDayResolver.DayScope.notAsked();
        if (day.failed()) {
            return Map.of(VARIABLE, day.message());
        }

        List<EmployeeResponse> candidates = findCandidates(tokens, day.dayKey());
        if (candidates.isEmpty()) {
            return Map.of(VARIABLE, NOT_FOUND);
        }

        // Skor: adayin ad kelimelerinden kaci mesajda geciyor. "ayse kaya" -> Ayse Kaya 2 puan,
        // yalnizca "kaya" gecen bir baska calisan 1 puan alir; tam ad her zaman one cikar.
        Map<EmployeeResponse, Long> scores = candidates.stream()
                .collect(Collectors.toMap(e -> e, e -> score(e, tokens), (a, b) -> a, LinkedHashMap::new));
        long best = scores.values().stream().max(Comparator.naturalOrder()).orElse(0L);

        List<EmployeeResponse> winners = scores.entrySet().stream()
                .filter(entry -> entry.getValue() == best)
                .map(Map.Entry::getKey)
                .toList();

        if (winners.size() > 1) {
            return Map.of(VARIABLE, ambiguityQuestion(winners));
        }
        return Map.of(VARIABLE, answerFor(winners.get(0), field, day.prefix()));
    }

    /**
     * Sira onemli: "nerede calisiyor" DEPARTMENT, tek basina "nerede" ise OFFICE_STATUS
     * demektir. Departman ipuclari once bakildigi icin uzun kalip kazanir.
     */
    private Field detectField(String foldedText) {
        if (containsAny(foldedText, PHONE_CUES)) {
            return Field.PHONE;
        }
        if (containsAny(foldedText, EMAIL_CUES)) {
            return Field.EMAIL;
        }
        if (containsAny(foldedText, DEPARTMENT_CUES)) {
            return Field.DEPARTMENT;
        }
        if (containsAny(foldedText, STATUS_CUES)) {
            return Field.OFFICE_STATUS;
        }
        return null;
    }

    private boolean containsAny(String foldedText, List<String> cues) {
        return cues.stream().anyMatch(foldedText::contains);
    }

    private String answerFor(EmployeeResponse employee, Field field, String dayPrefix) {
        if (field == null) {
            return employeeCard(employee);
        }
        String value = switch (field) {
            case PHONE -> employee.getPhone();
            case EMAIL -> employee.getEmail();
            case DEPARTMENT -> employee.getDepartmentName();
            case OFFICE_STATUS -> employee.getOfficeStatus();
        };
        // Alan bossa soruyu cevapsiz birakmak yerine elde ne varsa gosterilir: kullanici
        // bilgiyi baska bir alandan cikarabilir (or. telefonu yoksa e-postasi ise yarar).
        if (value == null || value.isBlank()) {
            return employee.getName() + " için kayıtlı " + field.label + " bilgisi yok. "
                    + "Rehberdeki bilgileri:\n" + employeeCard(employee);
        }
        if (field == Field.OFFICE_STATUS) {
            // A-40 (#209): gun soruldugunda "şu an" YANLIS olur — yanit baska bir gunun
            // durumunu gosteriyor. Onceki hali hem yanlis gunu donduruyor hem de onu
            // "şu an" diye sunuyordu; kullanicinin fark etmesi imkansizdi.
            return employee.getName() + " " + (dayPrefix.isEmpty() ? "şu an " : dayPrefix)
                    + value + " görünüyor.";
        }
        return employee.getName() + " — " + field.label + ": " + value;
    }

    private List<String> nameTokens(String foldedText) {
        return List.of(foldedText.split("[^a-z0-9]+")).stream()
                .filter(token -> token.length() >= MIN_TOKEN_LENGTH)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    // Her aday kelime icin rehber sorgusu; tum calisanlari cekip bellekte aramak yerine
    // mevcut LIKE sorgusu kullanilir (buyuk rehberde her mesajda tam tablo cekilmez).
    private List<EmployeeResponse> findCandidates(List<String> tokens, String dayKey) {
        Map<Integer, EmployeeResponse> byId = new LinkedHashMap<>();
        for (String token : tokens) {
            directoryService.searchEmployees(token, null, null, dayKey, 0, MAX_CANDIDATES).data()
                    .forEach(employee -> byId.putIfAbsent(employee.getId(), employee));
        }
        return new ArrayList<>(byId.values());
    }

    private long score(EmployeeResponse employee, List<String> tokens) {
        List<String> nameWords = List.of(TurkishText.foldToAscii(employee.getName()).split("[^a-z0-9]+"));
        return nameWords.stream().filter(tokens::contains).count();
    }

    private String ambiguityQuestion(List<EmployeeResponse> winners) {
        return "Birden fazla kişi eşleşti, hangisini kastettin?\n" + winners.stream()
                .map(e -> "• " + e.getName()
                        + (e.getDepartmentName() != null ? " (" + e.getDepartmentName() + ")" : ""))
                .collect(Collectors.joining("\n"));
    }

    // NULL alanlar acik metinle basilir; ham "null" kullaniciya gorunmemeli.
    private String employeeCard(EmployeeResponse employee) {
        return employee.getName() + "\n"
                + "• Departman: " + orUnknown(employee.getDepartmentName()) + "\n"
                + "• Telefon: " + orUnknown(employee.getPhone()) + "\n"
                + "• E-posta: " + orUnknown(employee.getEmail()) + "\n"
                + "• Ofis durumu: " + orUnknown(employee.getOfficeStatus());
    }

    private String orUnknown(String value) {
        return value == null || value.isBlank() ? UNKNOWN_FIELD : value;
    }
}
