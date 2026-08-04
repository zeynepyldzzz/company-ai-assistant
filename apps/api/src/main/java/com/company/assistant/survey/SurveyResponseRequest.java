package com.company.assistant.survey;

/**
 * POST /surveys/{id}/responses govdesi.
 * C-13 (#121): serbest "answers" map'i yerine sabit secenege (optionId) referans.
 */
public record SurveyResponseRequest(Integer optionId) {
}
