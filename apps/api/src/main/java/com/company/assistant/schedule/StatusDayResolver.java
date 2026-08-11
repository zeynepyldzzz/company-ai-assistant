package com.company.assistant.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.company.assistant.common.DateExpression;
import com.company.assistant.common.DayExpression;
import com.company.assistant.common.TurkishText;

/**
 * A-40 (#209): "bu soru hangi GUNUN durumunu istiyor" karari — tek yerde.
 *
 * <p><b>Var olma sebebi.</b> {@link DayExpression} ham tarihi cozuyor; ama bir DURUM sorgusu
 * icin tarih tek basina yetmiyor. Ayrica su dort karar gerekiyor: hafta disina tasti mi, hafta
 * sonuna denk geldi mi, haftalik toplu gorunum mu istendi, ve yanit hangi gunu gosterdigini
 * soylemeli mi. A-38'de bu blok {@code OfficeStatusVariableResolver}'in icine yazilmisti;
 * A-40'ta ayni blogun iki resolver'a daha gerekmesi onu kopyalanacak hale getirdi.
 *
 * <p>Bu projede o kopyalama tam olarak nasil bittigini biliyoruz: A-37'de "taninmayan tarih
 * sessizce bugune duser" hatasi IKI resolver'da birden yasiyordu, cunku mantik kopyalanmisti.
 * A-38 goreli gun cikarimini {@link DayExpression}'a tasidi; bu sinif da onun uzerindeki
 * karar katmanini tasiyor.
 *
 * <p><b>Sozlesme:</b> {@link DayScope#failed()} true ise cagiran taraf {@link DayScope#message()}
 * degerini AYNEN kullaniciya dondurmeli ve sorguyu HIC calistirmamali. Bugune dusmek yasak —
 * issue'nun tamami bu davranisin maliyeti uzerine.
 */
@Component
public class StatusDayResolver {

    /**
     * Mesajlar {@code ScheduleVariableResolver}'daki karsiliklariyla ayni ayrimi yapiyor ama
     * metinleri bilerek farkli: orada soru kullanicinin KENDI plani hakkinda ("çalışma
     * düzenin"), burada BASKALARI hakkinda ("sorduğun gün").
     */
    private static final String UNRESOLVED_DATE =
            "Hangi günü sorduğunu tam anlayamadım. \"bugün\", \"yarın\" ya da \"çarşamba\" "
                    + "gibi yazabilirsin.";
    private static final String ONLY_CURRENT_WEEK =
            "Şu an yalnızca içinde bulunduğumuz haftanın durumunu görebiliyorum.";
    private static final String WEEKEND =
            "Çalışma düzeni yalnızca Pazartesi-Cuma günleri için tanımlanıyor; sorduğun gün "
                    + "hafta sonuna denk geliyor.";
    /**
     * A-40: "bu hafta kimler ofiste" sorusu sessizce BUGUNU donduruyordu — ustelik gun ipucu
     * bulunamadigi icin yanitta gun etiketi de olmuyordu, yani kullanicinin fark etme sansi
     * sifirdi. Haftalik toplu durum gorunumu ayri bir yetenek; burada dogru olan onu
     * uydurmak degil, olmadigini soylemek.
     */
    private static final String WEEK_SCOPE =
            "Haftalık toplu durum görünümü veremiyorum. Belirli bir gün sorabilirsin — "
                    + "örneğin \"çarşamba kimler ofiste\".";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    private final TodayStatusService todayStatusService;

    public StatusDayResolver(TodayStatusService todayStatusService) {
        this.todayStatusService = todayStatusService;
    }

    /**
     * @param foldedText {@link TurkishText#foldToAscii} cikisi olmali
     */
    public DayScope resolve(String foldedText) {
        // Hafta kapsami ONCE: "bu hafta" ifadesinde tekil gun ipucu yoktur ve asagidaki
        // cozumleme onu sessizce bugune dusururdu. "haftaya çarşamba" bu daldan GECER
        // (tekil gun ipucu var) ve asagida hafta siniri kontrolune takilir — dogrusu da o.
        if (foldedText.contains("hafta") && !DayExpression.hasSingleDayCue(foldedText)) {
            return failure(WEEK_SCOPE);
        }

        LocalDate today = todayStatusService.today();
        Optional<LocalDate> resolved = DayExpression.resolveTargetDay(foldedText, today);
        if (resolved.isEmpty()) {
            // Tarih ifadesi VAR ama cozulemedi (or. yalniz "agustos", "32 agustos").
            return failure(UNRESOLVED_DATE);
        }
        LocalDate target = resolved.get();

        // statusesForDay yalnizca icinde bulunulan haftayi okuyor (schedule_day + weekStart);
        // disina tasan bir gun icin bu haftadan bir gun gostermek yanlis cevaptir.
        LocalDate weekStart = todayStatusService.currentWeekStart();
        if (target.isBefore(weekStart) || target.isAfter(weekStart.plusDays(6))) {
            return failure(ONLY_CURRENT_WEEK);
        }
        if (target.getDayOfWeek() == DayOfWeek.SATURDAY || target.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return failure(WEEKEND);
        }

        return new DayScope(
                target.getDayOfWeek().name().toLowerCase(Locale.ROOT),
                prefixFor(foldedText, target),
                null);
    }

    /**
     * Gun ACIKCA soruldugunda yanit hangi gunu gosterdigini SOYLEMELI — A-38'in tespiti buydu:
     * yanlis gun donerken kullanicinin fark etmesinin hicbir yolu yoktu.
     *
     * <p>Gun belirtilmemisse onek BOS kalir ve yanit metni aynen eskisi gibi durur; orada bugun
     * zaten dogru cevap ve gunu yazmak gurultu olur.
     */
    private String prefixFor(String foldedText, LocalDate target) {
        boolean mentionsDay = DayExpression.hasSingleDayCue(foldedText)
                || DateExpression.mentionsDate(foldedText);
        return mentionsDay
                ? TurkishText.dayName(target.getDayOfWeek()) + " (" + target.format(DATE_FMT) + ") günü "
                : "";
    }

    private DayScope failure(String message) {
        return new DayScope(null, "", message);
    }

    /**
     * @param dayKey  {@code schedule_day.day_of_week} bicimi (kucuk harf Ingilizce); hata
     *                durumunda {@code null}
     * @param prefix  yanit basligina eklenecek gun oneki; gun belirtilmemisse bos dize
     * @param message hata varsa kullaniciya AYNEN donecek metin, yoksa {@code null}
     */
    public record DayScope(String dayKey, String prefix, String message) {

        /**
         * Gun cikariminin HIC yapilmadigi durum — sorunun gunle ilgisi yoksa kullanilir
         * (or. "ayşe kaya telefonu"). {@code dayKey = null} DirectoryService'te bugune
         * karsilik gelir.
         *
         * <p>Neden ayri bir uretici: gun cikarimini boyle sorulara da uygulamak, cozulemeyen
         * bir tarih yuzunden telefon sorusunun bloklanmasi demekti — soru zaten gunle ilgili
         * degil.
         */
        public static DayScope notAsked() {
            return new DayScope(null, "", null);
        }

        /** True ise sorgu HIC calistirilmamali; {@link #message()} dondurulmeli. */
        public boolean failed() {
            return message != null;
        }
    }
}
