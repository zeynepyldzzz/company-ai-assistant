package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

/** POST /admin/surveys, GET /admin/surveys ve PUT /admin/surveys/{id}/publish cevabi. */
public record AdminSurveyResponse(Integer id, String title, boolean published, LocalDateTime createdAt,
                                   LocalDateTime deadline, List<SurveyOptionDto> options) {

    static AdminSurveyResponse from(Survey survey, List<SurveyOption> options) {
        return new AdminSurveyResponse(
                survey.getId(),
                survey.getTitle(),
                survey.isPublished(),
                survey.getCreatedAt(),
                survey.getDeadline(),
                options.stream().map(SurveyOptionDto::from).toList());
    }
}
