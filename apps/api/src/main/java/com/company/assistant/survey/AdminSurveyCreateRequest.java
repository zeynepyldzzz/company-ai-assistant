package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * POST /admin/surveys govdesi.
 * C-13 (#121): deadline (opsiyonel) ve sabit secenek listesi (min 2) eklendi.
 */
public record AdminSurveyCreateRequest(
        // A-44 (#219): sinirlarin kaynagi migration'lar — V1: survey.title VARCHAR(255),
        // V40: survey_option.option_text VARCHAR(255).
        @Size(max = 255, message = "Anket başlığı 255 karakteri aşamaz") String title,
        LocalDateTime deadline,
        List<@Size(max = 255, message = "Seçenek metni 255 karakteri aşamaz") String> options) {
}
