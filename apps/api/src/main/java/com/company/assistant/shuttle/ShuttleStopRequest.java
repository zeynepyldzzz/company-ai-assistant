package com.company.assistant.shuttle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ShuttleStopRequest(
        @NotBlank(message = "Durak adı boş olamaz")
        String name,

        @NotNull(message = "Durak saati zorunludur")
        LocalTime time,

        @NotNull(message = "Sıra numarası zorunludur")
        Integer orderIndex
) {}
