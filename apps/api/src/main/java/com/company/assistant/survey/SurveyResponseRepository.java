package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Integer> {

    /** C-8 (#52): GET /admin/surveys/{id}/results icin bir anketin tum yanitlari. */
    List<SurveyResponse> findAllBySurveyId(Integer surveyId);
}
