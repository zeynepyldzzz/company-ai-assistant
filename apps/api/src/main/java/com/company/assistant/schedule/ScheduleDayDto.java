package com.company.assistant.schedule;

/**
 * Haftanın tek bir gününü temsil eder: { "day": "monday", "status": "office" }
 */
public record ScheduleDayDto(String day, ScheduleStatus status) {
}