package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * PUT /admin/knowledge-base/documents/{id} govdesi (FR-78, 58).
 * Mevcut dokumana yeni bir versiyon ekler; eski versiyonlar is_current=false yapilir,
 * yenisi is_current=true olur. version_no otomatik (max+1).
 */
public record VersionCreateRequest(
        String content,
        List<ProcedureStep> steps,
        @NotNull(message = "effectiveDate zorunlu") LocalDate effectiveDate) {
}
