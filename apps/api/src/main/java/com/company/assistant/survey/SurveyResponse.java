package com.company.assistant.survey;

import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * V1__init.sql: survey_response(id, survey_id, employee_id NULLABLE, answers JSONB)
 * employee_id NULLABLE: FR-42 anket yanitlari giris yapmis calisandan gelir,
 * ama semada anonim yanit da desteklenir (bu issue'da her zaman JWT'den doldurulur).
 */
@Entity
@Table(name = "survey_response")
public class SurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "employee_id")
    private Integer employeeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers")
    private Map<String, Object> answers;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Survey getSurvey() { return survey; }
    public void setSurvey(Survey survey) { this.survey = survey; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public Map<String, Object> getAnswers() { return answers; }
    public void setAnswers(Map<String, Object> answers) { this.answers = answers; }
}
