package com.company.assistant.hr;

import java.time.Instant;
import java.time.LocalDate;

/**
 * GET /admin/knowledge-base/documents liste elemani.
 * currentVersionNo/currentEffectiveDate, guncel versiyonu olmayan dokuman icin null olabilir
 * (LEFT JOIN); normal akista POST her zaman v1 current olusturur.
 */
public record DocumentSummary(
        int id,
        int procedureId,
        String title,
        String procedureCategory,
        Integer currentVersionNo,
        LocalDate currentEffectiveDate,
        Instant createdAt) {
}
