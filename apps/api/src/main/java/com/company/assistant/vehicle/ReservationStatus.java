package com.company.assistant.vehicle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Rezervasyon durumu (FR-40). API'de kucuk harfle tasinir: confirmed / cancelled.
 */
public enum ReservationStatus {
    CONFIRMED,
    CANCELLED;

    @JsonCreator
    public static ReservationStatus fromApiValue(String value) {
        return ReservationStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
