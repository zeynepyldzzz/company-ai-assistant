package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

/**
 * POST /admin/surveys govdesi.
 * C-13 (#121): deadline (opsiyonel) ve sabit secenek listesi (min 2) eklendi.
 */
public record AdminSurveyCreateRequest(String title, LocalDateTime deadline, List<String> options) {
}
