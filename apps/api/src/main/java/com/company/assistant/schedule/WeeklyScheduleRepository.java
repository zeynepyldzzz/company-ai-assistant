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
}