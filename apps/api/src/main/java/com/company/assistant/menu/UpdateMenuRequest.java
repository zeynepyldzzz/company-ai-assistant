package com.company.assistant.menu;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record UpdateMenuRequest(
        @NotEmpty(message = "En az bir yemek gereklidir") List<@Valid MealItemRequest> items
) {}
