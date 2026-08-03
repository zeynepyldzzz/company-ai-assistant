package com.company.assistant.survey;

import java.util.List;
import java.util.Map;

/**
 * GET /admin/surveys/{id}/results cevabi.
 * FR-44: yetkili kullanicilar anket sonuclarini gorebilmeli.
 * C-13 (#121): "answers" serbest map yerine sabit secenek -> oy sayisi (answerCounts,
 * tek soruluk anket oldugu icin ic ic map yerine duz map yeterli).
 */
public record SurveyResultsResponse(
        Integer surveyId,
        String title,
        boolean published,
        int totalResponses,
        int totalFeedback,
        Map<String, Long> answerCounts,
        List<String> feedbackComments) {
}
