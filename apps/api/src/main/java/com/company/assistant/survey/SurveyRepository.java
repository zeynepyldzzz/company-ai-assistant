package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Integer> {

    /** C-8 (#52): GET /surveys/active — SADECE yayimlanmis (published=true) anketler. */
    List<Survey> findAllByPublishedTrueOrderByCreatedAtDesc();

    /** C-8 (#52): GET /admin/surveys — admin taslak+yayimlanmis TUM anketleri gorur. */
    List<Survey> findAllByOrderByCreatedAtDesc();
}
