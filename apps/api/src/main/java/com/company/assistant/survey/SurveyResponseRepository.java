package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Integer> {

    /** C-8 (#52): GET /admin/surveys/{id}/results icin bir anketin tum yanitlari. */
    List<SurveyResponse> findAllBySurveyId(Integer surveyId);

    /** C-13 (#121): calisana acik response-count endpoint'i icin (progress bar). */
    long countBySurveyId(Integer surveyId);

    /** C-13 (#121): anket silinirken once bagli yanitlari temizlemek icin (FK constraint). */
    void deleteAllBySurveyId(Integer surveyId);
}
