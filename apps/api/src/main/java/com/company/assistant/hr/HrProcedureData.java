package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

/**
 * HrProcedureVariableResolver icin ic tasiyici (REST DTO'su degildir).
 * <p>
 * hr_procedure satiri bulundugunda title/departman/iletisim her zaman doludur.
 * Guncel (is_current) policy_version yoksa versionNo/effectiveDate/steps null olur;
 * bu, ChatMessageService'in fallback template'ine dusmesi icin sinyaldir (dokuman §2).
 */
public record HrProcedureData(
        String title,
        String responsibleDepartment,
        String responsibleContact,
        Integer versionNo,
        LocalDate effectiveDate,
        List<ProcedureStep> steps) {

    public boolean hasCurrentVersion() {
        return versionNo != null;
    }
}
