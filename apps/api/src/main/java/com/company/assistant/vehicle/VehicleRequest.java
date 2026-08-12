package com.company.assistant.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// A-44 (#219): sinirlarin kaynagi V1__init.sql (plate VARCHAR(20), model VARCHAR(100)).
public record VehicleRequest(
        @NotBlank(message = "Plaka boş olamaz")
        @Size(max = 20, message = "Plaka 20 karakteri aşamaz")
        String plate,

        @Size(max = 100, message = "Model 100 karakteri aşamaz")
        String model,

        MaintenanceStatus maintenanceStatus
) {}
