package com.company.assistant.shuttle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ShuttleStopRequest(
        // A-44 (#219): kaynak V1__init.sql, shuttle_stop.name VARCHAR(150).
        @NotBlank(message = "Durak adı boş olamaz")
        @Size(max = 150, message = "Durak adı 150 karakteri aşamaz")
        String name,

        @NotNull(message = "Durak saati zorunludur")
        LocalTime time,

        @NotNull(message = "Sıra numarası zorunludur")
        Integer orderIndex,

        Double latitude,

        Double longitude
) {}
