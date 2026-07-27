package com.company.assistant.vehicle;

import jakarta.validation.constraints.NotBlank;

public record VehicleRequest(
        @NotBlank(message = "Plaka boş olamaz")
        String plate,

        String model,

        MaintenanceStatus maintenanceStatus
) {}
