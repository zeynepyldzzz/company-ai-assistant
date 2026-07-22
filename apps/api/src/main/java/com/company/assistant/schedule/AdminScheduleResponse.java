package com.company.assistant.schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * C-6: GET /admin/schedules cevabi.
 * Tum calisanlarin o haftaki duzeni (salt-okunur, tek kaynak - FR-64).
 */
public record AdminScheduleResponse(
        LocalDate weekStartDate,
        List<EmployeeScheduleDto> employees) {

    public record EmployeeScheduleDto(
            Integer employeeId,
            String employeeName,
            List<ScheduleDayDto> days) {
    }
}