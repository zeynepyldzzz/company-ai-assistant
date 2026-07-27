package com.company.assistant.survey;

import java.util.List;
import java.util.Map;

/**
 * GET /admin/surveys/{id}/results cevabi.
 * FR-44: yetkili kullanicilar anket sonuclarini gorebilmeli.
 * "Ozet/grafik halinde": her soru icin cevap degeri -> kac kisi verdigi
 * (answerCounts), boylece admin UI bunu bar/pie chart olarak cizebilir.
 */
public record SurveyResultsResponse(
        Integer surveyId,
        String title,
        boolean published,
        int totalResponses,
        int totalFeedback,
        Map<String, Map<String, Long>> answerCounts,
        List<String> feedbackComments) {
}
