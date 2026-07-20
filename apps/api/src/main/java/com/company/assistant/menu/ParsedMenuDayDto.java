package com.company.assistant.menu;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

/**
 * Excel'deki tek bir sütun (= tek bir gün) parse edildikten sonraki hali.
 */
public record ParsedMenuDayDto(
        LocalDate date,
        List<ParsedMealItemDto> items
) {
    public int weekNumber() {
        return date.get(WeekFields.of(new Locale("tr", "TR")).weekOfWeekBasedYear());
    }
}