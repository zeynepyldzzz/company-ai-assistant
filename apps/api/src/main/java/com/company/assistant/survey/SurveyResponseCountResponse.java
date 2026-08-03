package com.company.assistant.survey;

/**
 * C-13 (#121): GET /surveys/{id}/response-count cevabi.
 * Calisana acik, "kabul kriteri: response-count/ozet endpoint'i" icin - dashboard'daki
 * progress bar'a kac kisinin yanit verdigini (sonuc detayi olmadan) gosterir.
 */
public record SurveyResponseCountResponse(Integer surveyId, long totalResponses) {
}
