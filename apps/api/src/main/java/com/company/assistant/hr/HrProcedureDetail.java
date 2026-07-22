package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

// GET /hr/procedures?topic= ve GET /hr/procedures/{id} yaniti.
// Ozete ek olarak guncel versiyonun content + steps alanlarini tasir (FR-58).
public record HrProcedureDetail(
        int id,
        String title,
        String category,
        String responsibleDepartment,
        String responsibleContact,
        int versionNo,
        LocalDate effectiveDate,
        String content,
        List<ProcedureStep> steps) {
}
