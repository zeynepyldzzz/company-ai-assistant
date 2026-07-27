package com.company.assistant.survey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Integer> {

    /** C-7: GET /surveys/active — semada durum kolonu olmadigi icin tum anketler "aktif" sayilir. */
    List<Survey> findAllByOrderByCreatedAtDesc();
}
