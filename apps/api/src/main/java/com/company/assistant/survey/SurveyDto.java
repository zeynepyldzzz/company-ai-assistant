package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

/** GET /surveys/active listesindeki tek anket (calisan tarafi). */
public record SurveyDto(Integer id, String title, LocalDateTime createdAt, LocalDateTime deadline,
                         List<SurveyOptionDto> options) {

    static SurveyDto from(Survey survey, List<SurveyOption> options) {
        return new SurveyDto(
                survey.getId(),
                survey.getTitle(),
                survey.getCreatedAt(),
                survey.getDeadline(),
                options.stream().map(SurveyOptionDto::from).toList());
    }
}
