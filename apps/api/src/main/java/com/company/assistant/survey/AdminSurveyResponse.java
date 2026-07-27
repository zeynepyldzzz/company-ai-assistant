package com.company.assistant.survey;

import java.time.LocalDateTime;

/** POST /admin/surveys ve PUT /admin/surveys/{id}/publish cevabi. */
public record AdminSurveyResponse(Integer id, String title, boolean published, LocalDateTime createdAt) {

    static AdminSurveyResponse from(Survey survey) {
        return new AdminSurveyResponse(survey.getId(), survey.getTitle(), survey.isPublished(), survey.getCreatedAt());
    }
}
