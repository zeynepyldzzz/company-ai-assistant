package com.company.assistant.shuttle;

import jakarta.validation.constraints.NotBlank;

public record PlateUpdateRequest(
        @NotBlank(message = "Plaka bilgisi boş olamaz")
        String plateNumber
) {}
