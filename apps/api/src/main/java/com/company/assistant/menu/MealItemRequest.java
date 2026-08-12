package com.company.assistant.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record MealItemRequest(
        @NotNull(message = "Kategori boş olamaz") MealCategory category,
        // A-44 (#219): kaynak V1__init.sql, meal_item.name VARCHAR(150).
        @NotBlank(message = "Yemek adı boş olamaz")
        @Size(max = 150, message = "Yemek adı 150 karakteri aşamaz")
        String name
) {}
