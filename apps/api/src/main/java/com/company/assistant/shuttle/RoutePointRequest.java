package com.company.assistant.shuttle;

import jakarta.validation.constraints.NotNull;

public record RoutePointRequest(
        @NotNull Double lat,
        @NotNull Double lng
) {}
