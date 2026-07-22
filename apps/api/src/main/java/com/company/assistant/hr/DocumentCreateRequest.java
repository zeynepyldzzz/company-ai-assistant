package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /admin/knowledge-base/documents govdesi (FR-77/78).
 * Yeni bir policy_document + ilk versiyonu (v1, is_current=true) olusturur.
 * Gercek dosya binari alinmaz; icerik yapilandirilmis gelir (content + steps).
 */
public record DocumentCreateRequest(
        @NotNull(message = "procedureId zorunlu") Integer procedureId,
        @NotBlank(message = "title bos olamaz") String title,
        String content,
        List<ProcedureStep> steps,
        @NotNull(message = "effectiveDate zorunlu") LocalDate effectiveDate) {
}
