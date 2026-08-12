package com.company.assistant.shuttle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlateUpdateRequest(
        // A-44 (#219): kaynak V1__init.sql, shuttle_route.plate_number VARCHAR(20).
        @NotBlank(message = "Plaka bilgisi boş olamaz")
        @Size(max = 20, message = "Plaka 20 karakteri aşamaz")
        String plateNumber
) {}
