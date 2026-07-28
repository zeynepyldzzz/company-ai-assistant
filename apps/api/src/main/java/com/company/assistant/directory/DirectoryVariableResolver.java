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
            "bey", "beyin", "hanim", "hanimin", "bana", "lutfen", "acaba");

    private static final int MIN_TOKEN_LENGTH = 3;
    private static final int MAX_CANDIDATES = 25;

    private final DirectoryService directoryService;

    public DirectoryVariableResolver(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    public Map<String, String> resolve(String intentName, String message) {
        if (!DIRECTORY_INTENT.equals(intentName)) {
            return Map.of();
        }

        List<String> tokens = nameTokens(TurkishText.foldToAscii(message));
        if (tokens.isEmpty()) {
            return Map.of(VARIABLE, NO_NAME);
        }

        List<EmployeeResponse> candidates = findCandidates(tokens);
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
        return Map.of(VARIABLE, employeeCard(winners.get(0)));
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
    private List<EmployeeResponse> findCandidates(List<String> tokens) {
        Map<Integer, EmployeeResponse> byId = new LinkedHashMap<>();
        for (String token : tokens) {
            directoryService.searchEmployees(token, null, null, 0, MAX_CANDIDATES).data()
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
