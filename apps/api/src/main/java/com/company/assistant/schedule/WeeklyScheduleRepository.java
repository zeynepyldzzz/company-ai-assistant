package com.company.assistant.schedule;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Integer> {

    /**
     * Belirli bir calisanin belirli bir haftasini bulur.
     * FR-63'un temeli: sorgu her zaman employeeId ile yapilir,
     * kimse baskasinin kaydina ulasamaz.
     */
    Optional<WeeklySchedule> findByEmployeeIdAndWeekStartDate(Integer employeeId, LocalDate weekStartDate);
}