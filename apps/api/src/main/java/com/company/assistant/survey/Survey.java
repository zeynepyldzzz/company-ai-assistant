package com.company.assistant.survey;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * V1__init.sql: survey(id, title, created_by, created_at)
 * V20__add_survey_published.sql: survey.published (BOOLEAN, default false).
 * C-8 (#52): admin POST /admin/surveys ile taslak (published=false) anket
 * olusturur, PUT /admin/surveys/{id}/publish ile yayimlar. C-7'de (#51)
 * "semada kolon yok, tum anketler aktif" varsayimi bu kolonla duzeltildi:
 * GET /surveys/active artik SADECE published=true anketleri doner.
 */
@Entity
@Table(name = "survey")
public class Survey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
}
