package com.company.assistant.common;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A-37 (#203): mesajdaki ACIK tarih ifadelerini ("17 agustos", "17.08", "17.08.2026") tespit
 * eder ve cozer.
 *
 * <p>Var olma sebebi: resolver'lar taninmayan her ifadeyi sessizce BUGUNE dusuruyordu.
 * "Gun belirtilmemis" ile "gun belirtilmis ama cozulememis" ayni kod yoluna dusuyor, oysa
 * ilkinde bugun dogru cevap, ikincisinde yanlis. Kullanici 17 Agustos'u sorup bugunun
 * menusunu aliyor ve basligi okumazsa fark bile etmiyor.
 *
 * <p>Bu yuzden iki soru AYRI cevaplanmali: "ortada bir tarih ifadesi var mi?"
 * ({@link #mentionsDate}) ve "cozulebiliyor mu?" ({@link #resolve}). Ilki true, ikincisi bos
 * donuyorsa cagiran taraf varsayilana dusmemeli; durumu kullaniciya soylemeli.
 */
public final class DateExpression {

    /** foldToAscii cikisiyla ayni bicim: kucuk harf, aksansiz. */
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("ocak", 1),
            Map.entry("subat", 2),
            Map.entry("mart", 3),
            Map.entry("nisan", 4),
            Map.entry("mayis", 5),
            Map.entry("haziran", 6),
            Map.entry("temmuz", 7),
            Map.entry("agustos", 8),
            Map.entry("eylul", 9),
            Map.entry("ekim", 10),
            Map.entry("kasim", 11),
            Map.entry("aralik", 12));

    private static final String MONTH_ALTERNATION = String.join("|", MONTHS.keySet());

    /** "17 agustos", "17 agustos 2026" — Turkce'de en yaygin yazim. */
    private static final Pattern DAY_MONTH = Pattern.compile(
            "\\b(\\d{1,2})\\s+(" + MONTH_ALTERNATION + ")(?:\\s+(\\d{4}))?");

    /** "agustos 17" — daha nadir ama gecerli. */
    private static final Pattern MONTH_DAY = Pattern.compile(
            "\\b(" + MONTH_ALTERNATION + ")\\s+(\\d{1,2})\\b");

    /**
     * "17.08", "17/08/2026", "17-08".
     *
     * <p>Bilinen sinir: saat yazimi ("08.30") da bu desene uyar. Menu ve calisma duzeni
     * baglaminda saat sorulmadigi icin kabul edilebilir; ustelik ay araligi disinda kaldigi
     * (30 > 12) icin cozulemez ve kullaniciya sorulan seyin anlasilmadigi soylenir —
     * sessizce yanlis gun donmesinden iyidir.
     */
    private static final Pattern NUMERIC = Pattern.compile(
            "\\b(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?\\b");

    /** Yalniz ay adi: tarih ifadesi SAYILIR ama cozulemez ("agustos menusu" hangi gun?). */
    private static final Pattern BARE_MONTH = Pattern.compile(
            "\\b(" + MONTH_ALTERNATION + ")\\b");

    private static final List<Pattern> ALL =
            List.of(DAY_MONTH, MONTH_DAY, NUMERIC, BARE_MONTH);

    private DateExpression() {
    }

    /**
     * Mesajda acik bir tarih ifadesi geciyor mu.
     *
     * <p>Goreli ifadeler ("yarin", "carsamba", "haftaya") BURAYA DAHIL DEGIL — onlari
     * resolver'lar zaten cozuyor ve calisiyorlar.
     *
     * @param foldedText {@link TurkishText#foldToAscii} cikisi olmali
     */
    public static boolean mentionsDate(String foldedText) {
        if (foldedText == null) {
            return false;
        }
        return ALL.stream().anyMatch(pattern -> pattern.matcher(foldedText).find());
    }

    /**
     * Acik tarihi cozer. Tarih ifadesi yoksa ya da cozulemiyorsa bos doner; cagiran taraf
     * {@link #mentionsDate} ile bu iki durumu ayirt etmeli.
     *
     * @param reference yil belirtilmemisse kullanilacak referans (icinde bulunulan yil)
     */
    public static Optional<LocalDate> resolve(String foldedText, LocalDate reference) {
        if (foldedText == null) {
            return Optional.empty();
        }

        Matcher dayMonth = DAY_MONTH.matcher(foldedText);
        if (dayMonth.find()) {
            return build(dayMonth.group(3), MONTHS.get(dayMonth.group(2)),
                    Integer.parseInt(dayMonth.group(1)), reference);
        }

        Matcher monthDay = MONTH_DAY.matcher(foldedText);
        if (monthDay.find()) {
            return build(null, MONTHS.get(monthDay.group(1)),
                    Integer.parseInt(monthDay.group(2)), reference);
        }

        Matcher numeric = NUMERIC.matcher(foldedText);
        if (numeric.find()) {
            return build(numeric.group(3), Integer.parseInt(numeric.group(2)),
                    Integer.parseInt(numeric.group(1)), reference);
        }

        // Yalniz ay adi kaldi: hangi gun oldugu belirsiz, cozulemez.
        return Optional.empty();
    }

    private static Optional<LocalDate> build(String rawYear, Integer month, int day,
                                             LocalDate reference) {
        if (month == null) {
            return Optional.empty();
        }
        int year = reference.getYear();
        if (rawYear != null) {
            int parsed = Integer.parseInt(rawYear);
            // Iki haneli yil: 26 -> 2026. Gecmis yuzyili varsaymak bu baglamda anlamsiz.
            year = parsed < 100 ? 2000 + parsed : parsed;
        }
        try {
            return Optional.of(LocalDate.of(year, month, day));
        } catch (DateTimeException e) {
            // "32 agustos", "17.30" gibi gecersiz degerler: cozulemedi.
            return Optional.empty();
        }
    }
}
