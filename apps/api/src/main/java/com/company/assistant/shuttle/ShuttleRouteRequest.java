package com.company.assistant.shuttle;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ShuttleRouteRequest(
        @NotBlank(message = "Güzergah adı boş olamaz")
        String name,

        String plateNumber,

        @NotEmpty(message = "En az bir durak gereklidir")
        List<@Valid ShuttleStopRequest> stops
) {}
