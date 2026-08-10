package com.company.assistant.menu;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MealItemRequest(
        @NotNull(message = "Kategori boş olamaz") MealCategory category,
        @NotBlank(message = "Yemek adı boş olamaz") String name
) {}
