package com.company.assistant.hr;

import java.time.LocalDate;

// GET /hr/procedures liste yaniti. steps/content tasimaz (hafif ozet).
// Her ozet is_current = true versiyona dayanir (FR-58).
public record HrProcedureSummary(
        int id,
        String title,
        String category,
        String responsibleDepartment,
        String responsibleContact,
        int versionNo,
        LocalDate effectiveDate) {
}
