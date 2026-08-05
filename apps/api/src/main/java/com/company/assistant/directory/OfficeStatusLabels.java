package com.company.assistant.directory;

import com.company.assistant.schedule.ScheduleStatus;

/**
 * A-32 (#188): plan sozlugu ({@code OFFICE/REMOTE/LEAVE}) ile rehber sozlugu
 * ({@code Ofiste/Uzaktan/Izinde}) arasindaki ceviri.
 *
 * <p>Iki sozluk var cunku rehber degerleri API sozlesmesinin parcasi (packages/shared
 * {@code OfficeStatusSchema}) ve web istemcisi bunlari filtre parametresi olarak gonderiyor.
 * Plan tarafini enum'a cevirmek yerine ceviriyi tek noktada tutmak, sozlesmeyi ve frontend'i
 * degistirmeden veri kaynagini degistirmemizi sagliyor.
 *
 * <p>Ceviri BURADAN baska yerde yapilmamali; ayni esleme birden fazla yerde tekrarlanirsa
 * biri guncellenip digeri unutuldugunda sessizce yanlis filtre calisir.
 */
public final class OfficeStatusLabels {

    public static final String OFFICE = "Ofiste";
    public static final String REMOTE = "Uzaktan";
    public static final String LEAVE = "Izinde";

    private OfficeStatusLabels() {
    }

    /** Plan durumu -> rehber etiketi. */
    public static String labelFor(ScheduleStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OFFICE -> OFFICE;
            case REMOTE -> REMOTE;
            case LEAVE -> LEAVE;
        };
    }

    /**
     * Rehber etiketi -> plan durumu. Taninmayan deger {@code null} doner.
     *
     * <p>Taninmayan degerde exception ATILMAZ: bu ceviri bir query parametresinden besleniyor
     * ve kullanicinin elle yazdigi ?office=asdf sorgusu 500 vermemeli. Cagiran taraf null'i
     * "eslesme yok" olarak degerlendirir.
     */
    public static ScheduleStatus statusFor(String label) {
        if (label == null) {
            return null;
        }
        return switch (label) {
            case OFFICE -> ScheduleStatus.OFFICE;
            case REMOTE -> ScheduleStatus.REMOTE;
            case LEAVE -> ScheduleStatus.LEAVE;
            default -> null;
        };
    }
}
