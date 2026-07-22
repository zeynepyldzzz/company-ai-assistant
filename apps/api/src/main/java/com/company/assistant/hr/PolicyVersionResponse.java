package com.company.assistant.hr;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /admin/knowledge-base/documents/{id}/versions elemani (FR-58 denetim izi).
 * POST/PUT sonrasi olusan versiyonun yanitinda da kullanilir.
 */
public record PolicyVersionResponse(
        int id,
        int documentId,
        int versionNo,
        String content,
        List<ProcedureStep> steps,
        LocalDate effectiveDate,
        boolean isCurrent,
        Instant createdAt,
        Integer createdBy) {
}
