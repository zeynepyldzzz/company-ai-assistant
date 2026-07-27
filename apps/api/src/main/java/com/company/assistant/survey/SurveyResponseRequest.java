package com.company.assistant.survey;

import java.util.Map;

/** POST /surveys/{id}/responses govdesi: { "answers": { "soru1": "cevap1", ... } } */
public record SurveyResponseRequest(Map<String, Object> answers) {
}
