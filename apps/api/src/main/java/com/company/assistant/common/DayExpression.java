package com.company.assistant.common;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * A-38 (#207): mesajdaki GORELI gun ifadelerini ("yarin", "carsamba", "haftaya sali") cozer ve
 * {@link DateExpression} ile birlikte tek bir hedef tarih uretir.
 *
 * <p><b>Var olma sebebi:</b> bu mantik ScheduleVariableResolver'da private duruyordu ve
 * MenuVariableResolver'da bir kopyasi vardi. A-37'de duzeltilen hata (taninmayan tarihin
 * sessizce BUGUNE dusmesi) tam olarak bu yuzden IKI resolver'da birden yasiyordu — mantik
 * kopyalanmisti, dolayisiyla hata da kopyalanmisti. A-38 ucuncu bir tuketici getirdi
 * (OfficeStatusVariableResolver); ucuncu kopya yerine ortak kaynak.
 *
 * <p>{@code common} paketinde, cunku {@code schedule} ve {@code directory} katmanlari ayni
 * cikarimi yapmak zorunda ve aralarindaki bagimlilik tek yonlu: ScheduleVariableResolver
 * OfficeStatusVariableResolver'a delege eder, tersi mumkun degil (dongusel olurdu).
 * {@link TurkishText#mentionsThirdPersonGroup} ayni gerekceyle burada.
 *
 * <p><b>Kapsam disi:</b> MenuVariableResolver'in kendi varyanti bu sinifa TASINMADI — orada
 * "N gun sonra/once" gibi ek ipuclari var ve menu tarihi hafta siniriyla kisitli degil.
 * Birlestirmek A-37'nin menu davranisini riske atardi; ayri bir is.
 */
public final class DayExpression {

    private DayExpression() {
    }

    /**
     * Mesajin hedefledigi gun.
     *
     * <p><b>Bos Optional "tarih yok" DEMEK DEGILDIR</b> — "tarih ifadesi var ama cozulemedi"
     * demektir (or. yalniz "agustos", "32 agustos"). Cagiran taraf bu durumda BUGUNE DUSMEMELI;
     * kullaniciya anlamadigini soylemeli. Gun hic belirtilmemisse bugun doner, cunku o zaman
     * bugun dogru cevaptir. A-37'nin (#203) ayrimi budur ve tek yerde durmasi gerekiyor.
     *
     * @param foldedText {@link TurkishText#foldToAscii} cikisi olmali
     * @param today      referans gun; yil belirtilmemis acik tarihlerde de kullanilir
     */
    public static Optional<LocalDate> resolveTargetDay(String foldedText, LocalDate today) {
        if (DateExpression.mentionsDate(foldedText)) {
            return DateExpression.resolve(foldedText, today);
        }
        return Optional.of(resolveRelative(foldedText, today));
    }

    /** A-27 (#176): hafta ofseti gun cinsinden — gecmis -7, bu hafta 0, gelecek +7. */
    public static long weekShift(String foldedText) {
        if (isLastWeek(foldedText)) {
            return -7L;
        }
        return isNextWeek(foldedText) ? 7L : 0L;
    }

    public static boolean isNextWeek(String foldedText) {
        return foldedText.contains("gelecek") || foldedText.contains("onumuzdeki")
                || foldedText.contains("haftaya");
    }

    public static boolean isLastWeek(String foldedText) {
        return foldedText.contains("gecen hafta") || foldedText.contains("onceki hafta");
    }

    /** Mesajda TEKIL bir gun ipucu var mi — hafta listesi mi tek gun mu sorulduğunu ayirir. */
    public static boolean hasSingleDayCue(String foldedText) {
        if (foldedText.contains("bugun") || foldedText.contains("yarin")
                || foldedText.contains("obur gun") || foldedText.contains("dun")
                || foldedText.contains("onceki gun")) {
            return true;
        }
        return TurkishText.WEEKDAY_KEYWORDS.stream().anyMatch(e -> foldedText.contains(e.getKey()));
    }

    /**
     * Goreli ifadeden hedef tarih. Acik tarih burada ELE ALINMAZ; {@link #resolveTargetDay}
     * once onu dener.
     */
    private static LocalDate resolveRelative(String text, LocalDate today) {
        // A-27 (#176): hafta ofseti GORELI gun ipuclarina da uygulanir. Onceden yalnizca
        // hafta gunu adlarinda uygulaniyordu ve "haftaya bugün ofiste miyim" sorusu BUGUNUN
        // planini donduruyordu. Artik hedef gelecek/gecmis haftaya kayiyor; cagiran taraf
        // yalnizca icinde bulunulan haftayi verebiliyorsa sonuc "kapsam disi" mesaji olacak —
        // dogrusu da budur, sessizce yanlis gun gostermektense.
        long weekShift = weekShift(text);

        if (text.contains("yarin")) {
            return today.plusDays(1 + weekShift);
        }
        // #124: eskiden taninmiyordu ve varsayilan olarak BUGUNE dusuyordu — kullanici
        // yarindan sonrasini sorup bugunun cevabini aliyordu.
        if (text.contains("obur gun")) {
            return today.plusDays(2 + weekShift);
        }
        // A-20 (#139): gecmis yon. "Dün ofiste miydim" 0.763 ile DOGRU intent'e gidiyor
        // ama tarih cikarimi yoktu ve varsayilan olarak BUGUNE dusuyordu — kullanici dogru
        // formatta yanlis cevap aliyordu.
        if (text.contains("dun")) {
            return today.plusDays(-1 + weekShift);
        }
        if (text.contains("onceki gun") || text.contains("evvelki gun")) {
            return today.plusDays(-2 + weekShift);
        }
        // A-27: burada eskiden yalnizca nextWeek vardi; "geçen hafta çarşamba" BU haftanin
        // carsambasini donduruyordu.
        for (Map.Entry<String, DayOfWeek> entry : TurkishText.WEEKDAY_KEYWORDS) {
            if (text.contains(entry.getKey())) {
                LocalDate day = today.with(DayOfWeek.MONDAY).plusDays(entry.getValue().getValue() - 1L);
                return day.plusDays(weekShift);
            }
        }
        // "bugun" ve gun belirtilmemis durum -> bugun (hafta ofseti varsa kaydirilmis hali).
        return today.plusDays(weekShift);
    }
}
