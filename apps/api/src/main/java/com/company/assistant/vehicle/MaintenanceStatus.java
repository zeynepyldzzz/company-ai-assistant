package com.company.assistant.vehicle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Aracin bakim durumu (FR-41). API'de kucuk harfle tasinir: available / maintenance.
 */
public enum MaintenanceStatus {
    AVAILABLE,
    MAINTENANCE;

    @JsonCreator
    public static MaintenanceStatus fromApiValue(String value) {
        return MaintenanceStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
