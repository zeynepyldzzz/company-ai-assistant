package com.company.assistant.schedule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * PUT /schedules/me govdesinin is kurallarini kontrol eder (FR-60):
 * tam 5 is gunu, her gun bir kez, gecerli gun adlari, durum bos olamaz.
 */
@Component
public class ScheduleValidator {

    private static final Set<String> WORK_DAYS =
            Set.of("monday", "tuesday", "wednesday", "thursday", "friday");

    public void validate(WeeklyScheduleDto dto) {
        if (dto == null || dto.days() == null) {
            throw new IllegalArgumentException("Gövde boş olamaz");
        }
        List<ScheduleDayDto> days = dto.days();
        if (days.size() != 5) {
            throw new IllegalArgumentException("Tam 5 iş günü gönderilmeli (Pazartesi–Cuma)");
        }
        Set<String> seen = new HashSet<>();
        for (ScheduleDayDto day : days) {
            if (day.day() == null || !WORK_DAYS.contains(day.day().toLowerCase())) {
                throw new IllegalArgumentException("Geçersiz gün adı: " + day.day());
            }
            if (!seen.add(day.day().toLowerCase())) {
                throw new IllegalArgumentException("Gün birden fazla kez gönderilmiş: " + day.day());
            }
            if (day.status() == null) {
                throw new IllegalArgumentException("Durum boş olamaz: " + day.day());
            }
        }
    }
}