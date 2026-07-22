package com.company.assistant.schedule;

/**
 * GET /schedules/me/summary cevabı:
 * { "office": 3, "remote": 1, "leave": 1 }
 */
public record ScheduleSummaryDto(int office, int remote, int leave) {
}