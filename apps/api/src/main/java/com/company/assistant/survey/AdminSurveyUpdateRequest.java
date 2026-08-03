package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PUT /admin/surveys/{id} govdesi.
 * C-13 (#121): admin baslik, secenekler ve gecerlilik (deadline) tarihini duzenleyebilir.
 */
public record AdminSurveyUpdateRequest(String title, LocalDateTime deadline, List<String> options) {
}
