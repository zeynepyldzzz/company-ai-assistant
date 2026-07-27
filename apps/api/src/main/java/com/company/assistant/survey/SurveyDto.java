package com.company.assistant.survey;

import java.time.LocalDateTime;

/** GET /surveys/active listesindeki tek anket. */
public record SurveyDto(Integer id, String title, LocalDateTime createdAt) {

    static SurveyDto from(Survey survey) {
        return new SurveyDto(survey.getId(), survey.getTitle(), survey.getCreatedAt());
    }
}
