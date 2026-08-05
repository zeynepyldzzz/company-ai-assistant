package com.company.assistant.shuttle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RouteMatchRequest(
        @NotNull
        @Size(min = 2, message = "Eşleştirme için en az 2 nokta gereklidir")
        List<@Valid RoutePointRequest> points
) {}
