package com.company.assistant.schedule;

import java.time.LocalDate;
import java.util.List;

/**
 * GET/PUT /schedules/me gövdesi:
 * { "weekStartDate": "2026-07-20", "days": [ ... ] }
 */
public record WeeklyScheduleDto(LocalDate weekStartDate, List<ScheduleDayDto> days) {
}