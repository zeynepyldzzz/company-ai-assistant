package com.company.assistant.common;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chatbot resolver'larinin ortak Turkce metin/gun yardimcilari.
 *
 * A-11 (menu) ve A-12 (servis) bu mantigin kendi kopyasini tasiyordu; A-13 (calisma duzeni)
 * ucuncu kopya olacakti — o noktada duplikasyon tesadüf degil desen oldugu icin ortaklastirildi.
 * Kapsam bilerek dar: karakter katlama + gun adlari + gun anahtar kelimeleri. Her resolver'in
 * kendi alanina ozgu cikarimi ("N gun sonra", hafta modu, hat secimi) kendi sinifinda kalir.
 */
public final class TurkishText {

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Pazartesi",
            DayOfWeek.TUESDAY, "Salı",
            DayOfWeek.WEDNESDAY, "Çarşamba",
            DayOfWeek.THURSDAY, "Perşembe",
            DayOfWeek.FRIDAY, "Cuma",
            DayOfWeek.SATURDAY, "Cumartesi",
            DayOfWeek.SUNDAY, "Pazar");

    /**
     * Gun anahtar kelimeleri, eslestirme sirasiyla. Bilesik gunler once gelmeli:
     * "pazar" "pazartesi"nin, "cuma" "cumartesi"nin alt-dizesi.
     */
    public static final List<Map.Entry<String, DayOfWeek>> WEEKDAY_KEYWORDS = List.of(
            Map.entry("pazartesi", DayOfWeek.MONDAY),
            Map.entry("sali", DayOfWeek.TUESDAY),
            Map.entry("carsamba", DayOfWeek.WEDNESDAY),
            Map.entry("persembe", DayOfWeek.THURSDAY),
            Map.entry("cumartesi", DayOfWeek.SATURDAY),
            Map.entry("cuma", DayOfWeek.FRIDAY),
            Map.entry("pazar", DayOfWeek.SUNDAY));

    private TurkishText() {
    }

    /**
     * Embedding'e verilmeden once metni normallestirir: bas/son bosluk atilir ve Turkce
     * locale ile kucuk harfe cevrilir.
     *
     * A-17 (#124): normalizasyon yokken "Selamlar" 0.513, "selamlar" 0.797 benzerlik aliyordu —
     * ayni kelime, yalnizca ilk harf farki. Buyuk harfle baslayan sorular gereksiz yere esik
     * altinda kaliyordu. AYNI metot hem sorguda hem seed'de kullanilmali, aksi halde iki taraf
     * yine farkli normallesir.
     *
     * Turkce karakterler BILEREK katlanmaz: bge-m3 cok dilli bir model, "calisma"ya cevirmek
     * modele bozuk kelime vermek olur. foldToAscii yalnizca kelime eslestirme icindir.
     */
    public static String normalizeForEmbedding(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(new Locale("tr"));
    }

    /** Turkce gun adi: MONDAY -> "Pazartesi". */
    public static String dayName(DayOfWeek day) {
        return DAY_NAMES.get(day);
    }

    /**
     * Kucuk harfe cevirip Turkce karakterleri ASCII karsiliklarina katlar.
     * Boylece "Çarşamba" ve "carsamba" ayni sekilde eslesir.
     */
    public static String foldToAscii(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(new Locale("tr"))
                .replace('ç', 'c')
                .replace('ş', 's')
                .replace('ı', 'i')
                .replace('ğ', 'g')
                .replace('ü', 'u')
                .replace('ö', 'o');
    }
}
