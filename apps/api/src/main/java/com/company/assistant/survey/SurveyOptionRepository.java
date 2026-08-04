package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Integer> {

    /** C-13 (#121): anketi olustururken/goruntulerken secenekleri sirali dondurur. */
    List<SurveyOption> findAllBySurveyIdOrderBySortOrderAsc(Integer surveyId);

    /** C-13 (#121): anket duzenlenirken/silinirken eski secenekleri temizlemek icin. */
    void deleteAllBySurveyId(Integer surveyId);
}
