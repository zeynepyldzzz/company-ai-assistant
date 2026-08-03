package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    /** C-8 (#52): GET /admin/surveys/{id}/results icin bir ankete bagli anonim geri bildirimler. */
    List<Feedback> findAllBySurveyId(Integer surveyId);

    /** C-13 (#121): anket silinirken once bagli anonim geri bildirimleri temizlemek icin (FK constraint). */
    void deleteAllBySurveyId(Integer surveyId);
}
