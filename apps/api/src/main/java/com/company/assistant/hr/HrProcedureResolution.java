package com.company.assistant.hr;

import java.util.Map;

/**
 * HrProcedureVariableResolver sonucu. Uc durumu tasir:
 * <ul>
 *   <li>{@link #notApplicable()} — intent bir İK prosedurune bagli degil; merge edilecek
 *       degisken yok, normal akis surer.</li>
 *   <li>{@link #fallback()} — İK intent'i ama guncel versiyon yok; ChatMessageService
 *       fallback template'ine duser (dokuman §2), placeholder sizmaz.</li>
 *   <li>{@link #of(Map)} — İK-OK; 6 degiskenin hepsi dolu.</li>
 * </ul>
 */
public record HrProcedureResolution(Map<String, String> variables, boolean fallbackRequired) {

    private static final HrProcedureResolution NOT_APPLICABLE =
            new HrProcedureResolution(Map.of(), false);
    private static final HrProcedureResolution FALLBACK =
            new HrProcedureResolution(Map.of(), true);

    public static HrProcedureResolution notApplicable() {
        return NOT_APPLICABLE;
    }

    public static HrProcedureResolution fallback() {
        return FALLBACK;
    }

    public static HrProcedureResolution of(Map<String, String> variables) {
        return new HrProcedureResolution(variables, false);
    }
}
