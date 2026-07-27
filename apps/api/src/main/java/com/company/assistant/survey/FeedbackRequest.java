package com.company.assistant.survey;

/**
 * POST /feedback govdesi: { "surveyId": 3, "content": "..." }
 * FR-43 anonimlik: bilincli olarak employeeId ALANI YOK.
 * surveyId opsiyoneldir (genel geri bildirim, belirli bir ankete bagli olmayabilir).
 */
public record FeedbackRequest(Integer surveyId, String content) {
}
