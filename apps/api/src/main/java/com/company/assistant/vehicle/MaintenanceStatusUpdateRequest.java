package com.company.assistant.vehicle;

import jakarta.validation.constraints.NotNull;

public record MaintenanceStatusUpdateRequest(
        @NotNull(message = "Bakım durumu zorunludur")
        MaintenanceStatus maintenanceStatus
) {}
