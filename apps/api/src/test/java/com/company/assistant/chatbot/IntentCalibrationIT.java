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

            // --- A-20 (#139): kapsam/alan duzeltmeleri (chat_message_log, 2026-07-30) ---
            new Case("Ayşe Kaya ofiste mi", "rehber_kisi",
                    "0.610 intent_bulunamadi — ozel isim + durum, kural katmani"),
            new Case("Mehmet Demir nerede", "rehber_kisi", "tekil kisi durum sorusu"),
            new Case("ofiste olan kişiler", "calisma_duzeni",
                    "0.843 dogru intent'ti ama ucuncu sahis sayilmiyordu"),
            new Case("şirkette olan kişiler", "calisma_duzeni", "0.692 — sirket geneli kapsam"),
            new Case("dün ofiste miydim", "calisma_duzeni", "0.763 — gecmis gun"),
            new Case("Ayşe Kaya kimdir", "rehber_kisi", "0.565 — alan kelimesi yok"),
            new Case("ayşe kaya", "rehber_kisi", "0.581 — sirf isim"),
            new Case("şirkette olanları listele", "calisma_duzeni",
                    "0.643 — emir kipi; en yakini YANLIS intent'teydi (V32)"),
            new Case("ofiste olanları listele", "calisma_duzeni", "0.673 — esigin kil payi altinda"),
            new Case("ofiste olanları söyle", "calisma_duzeni", "0.616 — ikinci emir fiili"),

            // --- A-21 (#146): en yakin servis yonlendirmesi ---
            new Case("bana en yakın servis hangisi", "servis_en_yakin", "kural: en yakın + servis"),
            new Case("en yakın durak nerede", "servis_en_yakin", "kural"),
            new Case("kadıköy'e en yakın servis", "servis_en_yakin",
                    "durak adi geciyor ama yakinlik kurali oncelikli"),
            new Case("yakınımdaki servis durağı", "servis_en_yakin", "kisa form"),

            // --- A-24 (#155): tek kelimelik / kisa form sorgular ---
            // Ikisi ayni cozumu paylasiyor ama farkli semptom gosteriyordu: bir grupta
            // siralama dogruydu ve yalnizca esik yetmiyordu, digerinde kisa girdi anlamdan
            // bagimsiz olarak SELAMLAMAYA kaciyordu (uzunluk benzerligi anlami bastiriyor).
            new Case("Servisler", List.of("servis_guzergah", "servis_saatleri"),
                    "0.585 — iki servis intent'i de mesru"),
            new Case("Duraklar", "servis_guzergah", "0.494 — en yakini 'iyi günler' idi"),
            new Case("Menüler", "yemek_menusu", "0.656"),
            new Case("Yemekler", "yemek_menusu", "0.669 — esigin binde onbir altinda"),
            new Case("Departmanlar", "rehber_departman", "0.602"),
            new Case("Departmanları listele", "rehber_departman", "0.650 — emir kipi"),
            new Case("Rehber", List.of("rehber_kisi", "rehber_departman"),
                    "0.646 — en yakini 'merhaba' idi; iki rehber intent'i de mesru"),

            // Olculdu ve GECIYOR; yeni kisa formlar bunlari calmamali.
            new Case("Anketleri listele", "anket", "REGRESYON — 0.873"),
            new Case("Duyuruları göster", "duyurular", "REGRESYON — 0.812"),
            new Case("Çalışanları listele", "calisma_duzeni", "REGRESYON — 0.827"),

            // --- Capraz kirlenme nobetcileri: gun nitelemesi intent'ler arasi paylasilir ---
            // A-21 nobetcileri: uc servis intent'i de "servis" kelimesini paylasiyor,
            // ayrim yalnizca "en yakin" ifadesinde. En kirilgan yer burasi.
            new Case("kadıköy servisi kaçta", "servis_saatleri", "NOBETCI — A-21 calmamali"),
            new Case("servis saatleri", "servis_saatleri", "NOBETCI — vitrin sorusu (A-22)"),
            // A-20 nobetcisi: durum kelimesi ("ofiste"/"nerede") tasiyan LISTE sorulari
            // tek kisilik rehber yanitina KAYMAMALI.
            new Case("çalışanlar nerede", "calisma_duzeni", "NOBETCI — kural rehber_kisi'ye almamali"),
            // V32 emir kipi ornekleri TUM intent'lerin emir kipli sorularini calabilir;
            // #104'te V23 ornekleri "carsamba servis"i menuye calmisti.
            new Case("bu haftanın menüsünü listele", "yemek_menusu", "NOBETCI — emir kipi (V32)"),
            new Case("servisleri listele", List.of("servis_guzergah", "servis_saatleri"),
                    "NOBETCI — emir kipi (V32)"),
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
            new Case("bitcoin fiyatı ne kadar", NO_INTENT, "alakasiz"),
            // A-24 (#155): BILINCLI karar. Sistemde dort ayri prosedur kategorisi var
            // (izin, fazla mesai, oryantasyon, mazeret) ve bu sorgu hangisini kastettigini
            // soylemiyor. Belirsiz soruyu rastgele bir kategoriye zorlamaktansa
            // intent_bulunamadi donup A-22 oneri chip'lerini gostermek dogru davranis.
            new Case("Prosedürler", NO_INTENT, "0.503 — kasitli olarak eslesmiyor"),
            // A-24 (#155): "Hatlar" da kasitli olarak belirsiz birakildi. Ayni kalibin
            // digerleri gecti — "Servisler" ~ "servisler neler" 0.699, "Duraklar" ~
            // "duraklar neler" 0.686 — ama "Hatlar" ~ "hatlar neler" 0.628'in ALTINDA kaldi.
            // Ornek eksikligi degil: cumle zaten ekli ve embedding'i var. Sebep kelimenin
            // kendisi; "hat" Turkce'de cok anlamli (telefon hatti, cizgi, hat sanati) ve
            // model onu bizim kastettigimiz anlama sabitleyemiyor. Eklenecek her ornek ayni
            // duvara carpar. Tek basina "Hatlar" gercek kullanimda marjinal ve eslesmedigi
            // durumda A-22 oneri chip'leri devreye giriyor.
            new Case("Hatlar", NO_INTENT, "0.628 — kelime cok anlamli, kasitli olarak birakildi"));

    @Autowired
    private IntentClassificationService service;

    @Autowired
    private IntentSuggestionRepository suggestionRepository;

    /**
     * A-22 (#141): karsilama chip'lerinin HEPSI eslesmeli.
     *
     * <p>Chip'e tiklayan kullanicinin "anlamadim" yaniti almasi mumkun olan en kotu
     * deneyimdir — o soruyu kendimiz onerdik. Vaka listesine elle yazmak yerine DB'den
     * okunuyor: yeni bir chip eklendiginde bu test onu kendiliginden kapsar, birinin
     * test setini guncellemeyi hatirlamasi gerekmez.
     */
    @Test
    void vitrinSorularininHepsiEslesir() {
        List<String> failures = new ArrayList<>();
        List<ChatSuggestion> suggestions = suggestionRepository.findSuggestions();

        for (ChatSuggestion suggestion : suggestions) {
            var result = service.classify(suggestion.question());
            if (NO_INTENT.equals(result.intent())) {
                failures.add(String.format("%s -> %s (%.3f, en yakin: '%s')",
                        suggestion.question(), result.intent(),
                        result.similarity(), result.matchedPhrase()));
            }
        }

        assertThat(suggestions).as("Vitrin sorusu hic yok — seed uygulanmamis olabilir").isNotEmpty();
        assertThat(failures).as("Karsilama chip'leri intent_bulunamadi'ya dusuyor").isEmpty();
    }

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
