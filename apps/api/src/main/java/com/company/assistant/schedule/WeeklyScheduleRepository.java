package com.company.assistant.schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Integer> {

    /**
     * Belirli bir calisanin belirli bir haftasini bulur.
     * FR-63'un temeli: sorgu her zaman employeeId ile yapilir,
     * kimse baskasinin kaydina ulasamaz.
     */
    Optional<WeeklySchedule> findByEmployeeIdAndWeekStartDate(Integer employeeId, LocalDate weekStartDate);

    /**
     * C-6: Belirli bir haftanin TUM calisanlarinin duzenini getirir.
     * FR-64: ayni weekly_schedule tablosundan okur, kopya olusturmaz.
     * Gunler tek sorguda gelsin diye JOIN FETCH kullaniyoruz (N+1 onlemi).
     */
    @Query("""
        SELECT DISTINCT ws FROM WeeklySchedule ws
        LEFT JOIN FETCH ws.days
        WHERE ws.weekStartDate = :weekStart
        """)
    List<WeeklySchedule> findAllByWeekWithDays(LocalDate weekStart);

    /**
     * A-32 (#188): belirli bir gunde kimin hangi durumda oldugu.
     *
     * <p>{@code findAllByWeekWithDays} bu is icin de kullanilabilirdi ama haftanin BES gununu
     * birden getirir; cagiran taraf tek gun istiyorsa satirlarin %80'i atilir. Rehber her
     * listeleme isteginde bu sorguyu calistiracagi icin dar tutuldu.
     *
     * <p>{@code day_of_week} kolonu serbest metin; karsilastirma {@code LOWER()} ile yapiliyor
     * (ScheduleService kayitlari kucuk harf yaziyor, ama kolonda kisit yok).
     */
    @Query("""
        SELECT new com.company.assistant.schedule.EmployeeDayStatus(ws.employeeId, d.status)
        FROM WeeklySchedule ws
        JOIN ws.days d
        WHERE ws.weekStartDate = :weekStart
          AND LOWER(d.dayOfWeek) = :dayOfWeek
        """)
    List<EmployeeDayStatus> findStatusesByDay(LocalDate weekStart, String dayOfWeek);
}