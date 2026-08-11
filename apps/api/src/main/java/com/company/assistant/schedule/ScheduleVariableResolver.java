package com.company.assistant.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.company.assistant.common.ChatActions;
import com.company.assistant.common.DayExpression;
import com.company.assistant.common.TurkishText;
import com.company.assistant.directory.OfficeStatusVariableResolver;

/**
 * A-13 (FR-59..64): 'calisma_duzeni' intent'i icin kullanicinin KENDI haftalik ofis/uzaktan
 * planini uretir. MenuVariableResolver / ShuttleVariableResolver deseni; deterministik gun
 * cikarimi, LLM yok.
 *
 * Uretilen anahtar V25 template'iyle eslesir:
 *   {{calisma_duzenim}} -> tek gun cumlesi veya haftanin gun gun listesi
 *
 * Kimlik daima JWT'den alinir (FR-63, ScheduleController ile ayni kural) — mesajdaki isim
 * dikkate alinmaz. Baskasinin plani bu resolver'dan ASLA donmez; "kimler ofiste" tipi sorular
 * yalnizca /admin/schedules altinda ve bu issue'nun kapsami disinda (yetki sizintisi riski).
 *
 * Sinir (durum notu): ScheduleService yalnizca icinde bulunulan haftayi verir; gelecek hafta
 * sorulursa sessizce bu hafta donmek yerine acik mesaj basilir.
 */
@Component
public class ScheduleVariableResolver {

    private static final Logger log = LoggerFactory.getLogger(ScheduleVariableResolver.class);

    private static final String SCHEDULE_INTENT = "calisma_duzeni";
    private static final String VARIABLE = "calisma_duzenim";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    private static final String NO_IDENTITY =
            "Çalışma düzenini görebilmem için giriş yapmış olman gerekiyor.";
    private static final String ONLY_CURRENT_WEEK =
            "Şu an yalnızca içinde bulunduğumuz haftanın çalışma düzenini görebiliyorum.";
    private static final String NO_RECORD =
            "Bu hafta için kayıtlı bir çalışma düzenin görünmüyor. Çalışma Düzeni bölümünden planını girebilirsin.";
    /** A-37 (#203): tarih ifadesi var ama cozulemedi (or. yalniz "agustos", "32 agustos"). */
    private static final String UNRESOLVED_DATE =
            "Hangi tarihi sorduğunu tam anlayamadım. \"bugün\", \"yarın\" ya da \"çarşamba\" "
                    + "gibi yazabilirsin.";
    private static final String WEEKEND =
            "Çalışma düzeni yalnızca Pazartesi-Cuma günleri için tanımlanıyor; sorduğun gün hafta sonuna denk geliyor.";

    private final ScheduleService scheduleService;
    private final OfficeStatusVariableResolver officeStatusVariableResolver;

    public ScheduleVariableResolver(ScheduleService scheduleService,
                                    OfficeStatusVariableResolver officeStatusVariableResolver) {
        this.scheduleService = scheduleService;
        this.officeStatusVariableResolver = officeStatusVariableResolver;
    }

    public Map<String, String> resolve(String intentName, String message, Authentication authentication) {
        if (!SCHEDULE_INTENT.equals(intentName)) {
            return Map.of();
        }

        Integer employeeId = employeeId(authentication);
        if (employeeId == null) {
            // Degisken doldurulmadan birakilirsa ham {{calisma_duzenim}} kullaniciya gorunur.
            return answer(NO_IDENTITY, ChatActions.NONE);
        }

        String text = TurkishText.foldToAscii(message);

        // A-14 (#115): "kimler ofiste" tipi ucuncu sahis sorulari rehber verisinden yanitlanir
        // (employee.office_status, /employees ile employee roluna zaten acik). Kullanicinin
        // kendi haftalik plani asagidaki dalda kalir; weekly_schedule toplu gorunumu acilmaz.
        if (officeStatusVariableResolver.isThirdPersonQuestion(text)) {
            return Map.of(VARIABLE, officeStatusVariableResolver.resolve(text, employeeId));
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        WeeklyScheduleDto schedule = scheduleService.getMySchedule(employeeId);
        if (schedule.days().isEmpty()) {
            // A-30 (#185): yanitin kendisi "planini gir" diyor; buton da oraya goturmeli.
            // Varsayilan buton kullaniciyi calisan listesine goturuyordu — metinle celisiyordu.
            return answer(NO_RECORD, ChatActions.MY_SCHEDULE);
        }

        // Hafta modu: "hafta" gecti ya da "hangi gunler" soruldu ve tekil gun ipucu yok.
        if ((text.contains("hafta") || asksWholeWeek(text)) && !DayExpression.hasSingleDayCue(text)) {
            // #127: gecmis hafta da kapsam disi. Eskiden yalnizca gelecek hafta kontrol
            // ediliyordu, "gecen hafta calisma duzenim" BU haftanin planini donduruyordu.
            if (DayExpression.isNextWeek(text) || DayExpression.isLastWeek(text)) {
                return answer(ONLY_CURRENT_WEEK, ChatActions.NONE);
            }
            return answer(buildWeekBody(schedule), ChatActions.NONE);
        }

        // A-37 (#203): acik tarih once, cozulemezse BUGUNE DUSULMEZ. A-38 (#207): bu ayrim
        // DayExpression'a tasindi — ayni cikarimi ucuncu bir resolver da yapmak zorunda
        // (OfficeStatusVariableResolver) ve mantik kopyalandigi her seferde hata da
        // kopyalaniyordu.
        Optional<LocalDate> resolved = DayExpression.resolveTargetDay(text, today);
        if (resolved.isEmpty()) {
            return answer(UNRESOLVED_DATE, ChatActions.NONE);
        }
        LocalDate target = resolved.get();

        // Hedef bu haftanin disina tastiysa veri yok: sessizce bu haftayi dondurmek yanlis cevaptir.
        if (target.isBefore(weekStart) || target.isAfter(weekStart.plusDays(6))) {
            return answer(ONLY_CURRENT_WEEK, ChatActions.NONE);
        }
        if (target.getDayOfWeek() == DayOfWeek.SATURDAY || target.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return answer(WEEKEND, ChatActions.NONE);
        }

        String dayKey = target.getDayOfWeek().name().toLowerCase(Locale.ROOT);
        Optional<ScheduleDayDto> day = schedule.days().stream()
                .filter(d -> d.day().equalsIgnoreCase(dayKey))
                .findFirst();

        String label = TurkishText.dayName(target.getDayOfWeek()) + " (" + target.format(DATE_FMT) + ")";
        return answer(day
                .map(d -> label + " günü " + sentenceFor(d.status()))
                .orElse(label + " günü için kayıtlı bir durum yok."),
                ChatActions.NONE);
    }

    /**
     * A-30 (#185): yanit + o yanita ait buton hedefi.
     *
     * <p>Kullanicinin KENDI planini donduren her dal butonu acikca seciyor; hicbiri
     * intent'in varsayilan butonunu (calisan listesi) miras almiyor. Sebep: o buton
     * yalnizca ucuncu sahis dali ("kimler ofiste") icin tanimlanmisti ve kendi planini
     * soran kullaniciyi alakasiz bir ekrana goturuyordu. Ucuncu sahis dali override
     * BILDIRMEZ; varsayilani kullanmaya devam eder.
     */
    private Map<String, String> answer(String body, String actionTarget) {
        return Map.of(VARIABLE, body, ChatActions.OVERRIDE_KEY, actionTarget);
    }

    private Integer employeeId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            log.warn("Authentication name sayisal degil: {}", authentication.getName());
            return null;
        }
    }

    // "hangi gunler", "hangi gun" gibi cogul/soru kaliplari da tum haftayi ister; bunlar
    // "hafta" kelimesini icermedigi icin eskiden tek-gun dalina dusup BUGUNU donduruyordu (#124).
    // DayExpression'a TASINMADI: hafta listesi yalnizca bu resolver'in modu.
    private boolean asksWholeWeek(String text) {
        return text.contains("hangi gun") || text.contains("hangi gunler");
    }

    private String buildWeekBody(WeeklyScheduleDto schedule) {
        return "Bu haftanın çalışma düzeni:\n" + schedule.days().stream()
                .map(d -> "• " + dayLabel(d.day()) + " — " + statusLabel(d.status()))
                .collect(Collectors.joining("\n"));
    }

    // ScheduleService gunleri her zaman Pazartesi-Cuma sirasinda dondurur; ek siralama gereksiz.
    private String dayLabel(String apiDay) {
        try {
            return TurkishText.dayName(DayOfWeek.valueOf(apiDay.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            log.warn("Bilinmeyen schedule_day.day_of_week degeri: {}", apiDay);
            return apiDay;
        }
    }

    private String statusLabel(ScheduleStatus status) {
        return switch (status) {
            case OFFICE -> "Ofis";
            case REMOTE -> "Uzaktan";
            case LEAVE -> "İzinli";
        };
    }

    private String sentenceFor(ScheduleStatus status) {
        return switch (status) {
            case OFFICE -> "ofistesin.";
            case REMOTE -> "uzaktan çalışıyorsun.";
            case LEAVE -> "izinlisin.";
        };
    }
}
