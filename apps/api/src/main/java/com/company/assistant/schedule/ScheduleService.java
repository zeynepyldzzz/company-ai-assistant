package com.company.assistant.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    /** Gunlerin API'deki sabit sirasi (FR-60: Pazartesi-Cuma). */
    private static final List<String> DAY_ORDER =
            List.of("monday", "tuesday", "wednesday", "thursday", "friday");

    private final WeeklyScheduleRepository repository;
    private final ScheduleValidator validator;

    public ScheduleService(WeeklyScheduleRepository repository, ScheduleValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /** Icinde bulundugumuz haftanin Pazartesi tarihini dondurur. */
    static LocalDate currentWeekStart() {
        return LocalDate.now().with(DayOfWeek.MONDAY);
    }

    /**
     * GET /schedules/me — calisanin bu haftaki plani.
     * Kayit yoksa bos gunlerle (hepsi 'office' varsayilan) dondurmek yerine
     * gunleri hic doldurmadan donduruyoruz; frontend "henuz secim yok" gosterebilir.
     */
    @Transactional(readOnly = true)
    public WeeklyScheduleDto getMySchedule(Integer employeeId) {
        LocalDate weekStart = currentWeekStart();
        return repository.findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
                .map(this::toDto)
                .orElseGet(() -> new WeeklyScheduleDto(weekStart, List.of()));
    }

    /**
     * PUT /schedules/me — bu haftanin gunlerini kaydeder/gunceller.
     * FR-64: ayni (employeeId, weekStartDate) icin tek kayit — varsa gunceller, yoksa olusturur.
     */
    @Transactional
    public WeeklyScheduleDto saveMySchedule(Integer employeeId, WeeklyScheduleDto dto) {
        validator.validate(dto);
        LocalDate weekStart = currentWeekStart();

        WeeklySchedule schedule = repository
                .findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
                .orElseGet(() -> {
                    WeeklySchedule s = new WeeklySchedule();
                    s.setEmployeeId(employeeId);
                    s.setWeekStartDate(weekStart);
                    return s;
                });

       // Gunleri yerinde guncelle: mevcutsa status'unu degistir, yoksa ekle.
        // (clear() + yeniden ekleme, Hibernate'in INSERT'leri DELETE'lerden once
        // calistirmasi nedeniyle unique kisita takiliyordu.)
        for (String dayName : DAY_ORDER) {
            ScheduleDayDto dayDto = dto.days().stream()
                    .filter(d -> d.day().equalsIgnoreCase(dayName))
                    .findFirst()
                    .orElseThrow(); // validator 5 gunu garanti etti, buraya dusmez

            ScheduleDay existing = schedule.getDays().stream()
                    .filter(d -> d.getDayOfWeek().equalsIgnoreCase(dayName))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.setStatus(dayDto.status()); // yerinde guncelle
            } else {
                ScheduleDay day = new ScheduleDay();
                day.setSchedule(schedule);
                day.setDayOfWeek(dayName);
                day.setStatus(dayDto.status());
                schedule.getDays().add(day);
            }
        }

        return toDto(repository.save(schedule));
    }

    /** GET /schedules/me/summary — gun sayilari (FR-61). */
    @Transactional(readOnly = true)
    public ScheduleSummaryDto getMySummary(Integer employeeId) {
        WeeklyScheduleDto schedule = getMySchedule(employeeId);
        int office = 0, remote = 0, leave = 0;
        for (ScheduleDayDto day : schedule.days()) {
            switch (day.status()) {
                case OFFICE -> office++;
                case REMOTE -> remote++;
                case LEAVE -> leave++;
            }
        }
        return new ScheduleSummaryDto(office, remote, leave);
    }

    /** Entity -> DTO cevirisi, gunler her zaman Pazartesi-Cuma sirasinda doner. */
    private WeeklyScheduleDto toDto(WeeklySchedule schedule) {
        List<ScheduleDayDto> days = DAY_ORDER.stream()
                .map(dayName -> schedule.getDays().stream()
                        .filter(d -> d.getDayOfWeek().equalsIgnoreCase(dayName))
                        .findFirst()
                        .map(d -> new ScheduleDayDto(dayName, d.getStatus()))
                        .orElse(null))
                .filter(d -> d != null)
                .toList();
        return new WeeklyScheduleDto(schedule.getWeekStartDate(), days);
    }
}