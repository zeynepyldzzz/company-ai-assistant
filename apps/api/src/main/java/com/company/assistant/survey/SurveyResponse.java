package com.company.assistant.survey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * V1__init.sql: survey_response(id, survey_id, employee_id NULLABLE, answers JSONB)
 * C-13 (#121): serbest "answers" JSONB kaldirildi, sabit secenege (option_id) referans
 * eklendi. (survey_id, employee_id) uzerinde unique constraint var - bir calisan bir
 * ankete sadece bir kez oy verebilir, ikinci deneme DB'de unique violation'a duser,
 * SurveyService bunu yakalayip 409 (SurveyAlreadyRespondedException) dondurur.
 */
@Entity
@Table(name = "survey_response", uniqueConstraints = {
        @UniqueConstraint(name = "uq_survey_response_survey_employee", columnNames = {"survey_id", "employee_id"})
})
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "employee_id")
    private Integer employeeId;

    @ManyToOne
    @JoinColumn(name = "option_id")
    private SurveyOption option;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public SurveyOption getOption() { return option; }
    public void setOption(SurveyOption option) { this.option = option; }
}
