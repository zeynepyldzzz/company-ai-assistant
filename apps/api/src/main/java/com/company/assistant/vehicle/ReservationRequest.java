package com.company.assistant.vehicle;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotNull(message = "Araç seçimi zorunludur")
        Integer vehicleId,

        @NotNull(message = "Başlangıç zamanı zorunludur")
        LocalDateTime startTime,

        @NotNull(message = "Bitiş zamanı zorunludur")
        LocalDateTime endTime
) {}
