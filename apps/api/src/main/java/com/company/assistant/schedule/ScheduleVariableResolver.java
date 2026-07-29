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
            return Map.of(VARIABLE, NO_IDENTITY);
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
            return Map.of(VARIABLE, NO_RECORD);
        }

        // Hafta modu: "hafta" gecti ya da "hangi gunler" soruldu ve tekil gun ipucu yok.
        if ((text.contains("hafta") || asksWholeWeek(text)) && !hasSingleDayCue(text)) {
            // #127: gecmis hafta da kapsam disi. Eskiden yalnizca gelecek hafta kontrol
            // ediliyordu, "gecen hafta calisma duzenim" BU haftanin planini donduruyordu.
            if (isNextWeek(text) || isLastWeek(text)) {
                return Map.of(VARIABLE, ONLY_CURRENT_WEEK);
            }
            return Map.of(VARIABLE, buildWeekBody(schedule));
        }

        LocalDate target = resolveTarget(text, today);
        // Hedef bu haftanin disina tastiysa veri yok: sessizce bu haftayi dondurmek yanlis cevaptir.
        if (target.isBefore(weekStart) || target.isAfter(weekStart.plusDays(6))) {
            return Map.of(VARIABLE, ONLY_CURRENT_WEEK);
        }
        if (target.getDayOfWeek() == DayOfWeek.SATURDAY || target.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return Map.of(VARIABLE, WEEKEND);
        }

        String dayKey = target.getDayOfWeek().name().toLowerCase(Locale.ROOT);
        Optional<ScheduleDayDto> day = schedule.days().stream()
                .filter(d -> d.day().equalsIgnoreCase(dayKey))
                .findFirst();

        String label = TurkishText.dayName(target.getDayOfWeek()) + " (" + target.format(DATE_FMT) + ")";
        return Map.of(VARIABLE, day
                .map(d -> label + " günü " + sentenceFor(d.status()))
                .orElse(label + " günü için kayıtlı bir durum yok."));
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

    private boolean hasSingleDayCue(String text) {
        if (text.contains("bugun") || text.contains("yarin") || text.contains("obur gun")) {
            return true;
        }
        return TurkishText.WEEKDAY_KEYWORDS.stream().anyMatch(e -> text.contains(e.getKey()));
    }

    // "hangi gunler", "hangi gun" gibi cogul/soru kaliplari da tum haftayi ister; bunlar
    // "hafta" kelimesini icermedigi icin eskiden tek-gun dalina dusup BUGUNU donduruyordu (#124).
    private boolean asksWholeWeek(String text) {
        return text.contains("hangi gun") || text.contains("hangi gunler");
    }

    private boolean isNextWeek(String text) {
        return text.contains("gelecek") || text.contains("onumuzdeki") || text.contains("haftaya");
    }

    private boolean isLastWeek(String text) {
        return text.contains("gecen hafta") || text.contains("onceki hafta");
    }

    // Ham (ASCII'ye katlanmis) mesajdan hedef tarihi cikarir. Menudeki resolveTarget ile ayni
    // aile; "N gun sonra" burada yok, cunku plan zaten tek haftalik.
    private LocalDate resolveTarget(String text, LocalDate today) {
        if (text.contains("yarin")) {
            return today.plusDays(1);
        }
        // #124: eskiden taninmiyordu ve varsayilan olarak BUGUNE dusuyordu — kullanici
        // yarindan sonrasini sorup bugunun cevabini aliyordu.
        if (text.contains("obur gun")) {
            return today.plusDays(2);
        }
        boolean nextWeek = isNextWeek(text);
        for (Map.Entry<String, DayOfWeek> entry : TurkishText.WEEKDAY_KEYWORDS) {
            if (text.contains(entry.getKey())) {
                LocalDate day = today.with(DayOfWeek.MONDAY).plusDays(entry.getValue().getValue() - 1L);
                return nextWeek ? day.plusDays(7) : day;
            }
        }
        // "bugun" ve gun belirtilmemis durum -> bugun.
        return today;
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
