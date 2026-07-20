package com.company.assistant.menu;

/**
 * Excel'den okunan tek bir yemek satırı.
 * Kalori/alerjen bu şablonda hiç gelmiyor, o yüzden burada yok.
 */
public record ParsedMealItemDto(
        MealCategory category,
        String name
) {
}