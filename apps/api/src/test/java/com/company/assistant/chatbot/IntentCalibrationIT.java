package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * A-19 (#129): intent kalibrasyonunun OLCUM ARACI.
 *
 * IntentClassificationIT'ten farki: o, calisan davranisi dogrular ve kirmizi kalmamalidir.
 * Bu ise mevcut durumu OLCER — ornek zenginlestirmesi ilerledikce yesile doner. Amac,
 * "ornek ekledim, neyi kazandim neyi kaybettim" sorusunu elle deneyerek degil tek komutla
 * cevaplayabilmek.
 *
 * Tek assertion kullanilir: her vaka koşulur, sonunda tam rapor basilir. Parametreli test
 * olsaydi ilk basarisizlikta kalan vakalarin durumu gorunmezdi ve ilerleme olculemezdi.
 *
 * Kosma:
 *   .\mvnw test "-Dtest=IntentCalibrationIT" "-DINTENT_IT=true"
 * Kosul: docker compose up -d (db + ollama), seed tamamlanmis olmali.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "INTENT_IT", matches = "true")
class IntentCalibrationIT {

    /** Belirsiz girdilerin kalmasi gereken yer; eslesme YOK demektir. */
    private static final String NO_INTENT = IntentClassificationService.NO_INTENT;

    /**
     * Bazi sorular birden fazla intent'e mesru sekilde gidebilir: "kadikoy servisi" hem
     * saat hem guzergah yaniti icin makul. Tek dogru dayatmak testi gercek disi katilastirir.
     */
    private record Case(String query, List<String> acceptedIntents, String note) {

        Case(String query, String expectedIntent, String note) {
            this(query, List.of(expectedIntent), note);
        }

        boolean accepts(String intent) {
            return acceptedIntents.contains(intent);
        }

        String expected() {
            return String.join(" | ", acceptedIntents);
        }
    }

    /**
     * Vaka listesi. YALNIZCA bilinen basarisizliklar degil, HALA CALISMASI GEREKENLER de
     * burada — capraz kirlenme ancak boyle gorunur. #104'te V23 ornekleri "carsamba servis"i
     * menuye calmisti ve bu ancak elle fark edilmisti.
     */
    private static final List<Case> CASES = List.of(
            // --- Olculmus basarisizliklar (chat_message_log, 2026-07-29) ---
            new Case("anketler", "anket", "0.675 — esigin bes binde bir altinda"),
            new Case("duyurular", "duyurular", "kisa form"),
            new Case("kadıköy servisi", List.of("servis_guzergah", "servis_saatleri"),
                    "0.572 — yer adi + kisa form; iki servis intent'i de mesru"),
            new Case("muhasebe bölümü", "rehber_departman", "0.545 — 'bolum' ~ 'departman'"),
            new Case("muhasebe departmanı yetkilisi", "rehber_departman", "0.572"),
            new Case("kimler ofiste", "calisma_duzeni", "0.498 — ornegin tam alt-dizesi"),
            new Case("kimler uzaktan", "calisma_duzeni", "0.600"),
            new Case("çalışma düzenim", "calisma_duzeni", "kisa form"),
            new Case("geçen hafta çalışma düzenim", "calisma_duzeni",
                    "0.607 — en yakini REHBER cumlesi, siralama bozuk"),
            new Case("menü", "yemek_menusu", "kisa form"),
            new Case("dünkü yemek", "yemek_menusu", "gecmis kalibi"),

            // --- Capraz kirlenme nobetcileri: gun nitelemesi intent'ler arasi paylasilir ---
            new Case("çarşamba yemekte ne var", "yemek_menusu", "NOBETCI"),
            new Case("çarşamba servisi kaçta", "servis_saatleri", "NOBETCI"),
            new Case("çarşamba ofiste miyim", "calisma_duzeni", "NOBETCI"),

            // --- Calisiyor, bozulmamali ---
            new Case("bugün yemekte ne var", "yemek_menusu", "regresyon"),
            new Case("servis nereden geçiyor", "servis_guzergah", "regresyon"),
            new Case("aktif anket var mı", "anket", "regresyon"),
            new Case("muhasebeye nasıl ulaşırım", "rehber_departman", "regresyon"),
            new Case("Ayşe Kaya'nın dahilisi kaç", "rehber_kisi", "regresyon"),
            new Case("yarın ofiste miyim", "calisma_duzeni", "regresyon"),
            new Case("selam", "selamlama", "A-17 normalizasyonu"),
            new Case("Selamlar", "selamlama", "A-17 normalizasyonu"),
            new Case("34 SR 101", "servis_guzergah", "A-17 kural katmani"),

            // --- Belirsiz kalmali: eslestirmek YANLIS cevap uretir ---
            new Case("çarşamba", NO_INTENT, "tek basina belirsiz — 0.611 ile selamlamaya yakin"),
            new Case("ekran", NO_INTENT, "0.576"),
            new Case("bitcoin fiyatı ne kadar", NO_INTENT, "alakasiz"));

    @Autowired
    private IntentClassificationService service;

    @Test
    void kalibrasyonSeti() {
        List<String> failures = new ArrayList<>();
        List<String> report = new ArrayList<>();

        for (Case testCase : CASES) {
            var result = service.classify(testCase.query());
            boolean ok = testCase.accepts(result.intent());
            String line = String.format("%s %-32s -> %-20s (%.3f, en yakin: '%s') [%s]",
                    ok ? "OK  " : "FAIL",
                    testCase.query(),
                    result.intent(),
                    result.similarity(),
                    result.matchedPhrase(),
                    testCase.note());
            report.add(line);
            if (!ok) {
                failures.add(line + " | beklenen: " + testCase.expected());
            }
        }

        int passed = CASES.size() - failures.size();
        String summary = String.format("%nKALIBRASYON: %d/%d gecti (esik: %.2f)%n%s%n",
                passed, CASES.size(), service.getThreshold(), String.join("\n", report));

        assertThat(failures).as(summary).isEmpty();
    }
}
