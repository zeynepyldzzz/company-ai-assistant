package com.company.assistant.schedule;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Bir iş gününün çalışma durumu (FR-60).
 * API'de küçük harfle taşınır: office / remote / leave.
 */
public enum ScheduleStatus {
    OFFICE,
    REMOTE,
    LEAVE;

@JsonCreator
    /** API'den gelen "office" gibi küçük harfli değeri enum'a çevirir. */
    public static ScheduleStatus fromApiValue(String value) {
        return ScheduleStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

@JsonValue
    /** Enum'u API'nin beklediği küçük harfli hâline çevirir: OFFICE -> "office". */
    public String toApiValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}