package com.company.assistant.announcement;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * C-9 (#53): FR-65-67 calisan bildirim tercihleri. employee_id basina tek satir (UNIQUE).
 */
@Entity
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Integer employeeId;

    @Column(name = "announcement_enabled", nullable = false)
    private boolean announcementEnabled = true;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean scheduleEnabled = true;

    @Column(name = "survey_enabled", nullable = false)
    private boolean surveyEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public boolean isAnnouncementEnabled() {
        return announcementEnabled;
    }

    public void setAnnouncementEnabled(boolean announcementEnabled) {
        this.announcementEnabled = announcementEnabled;
    }

    public boolean isScheduleEnabled() {
        return scheduleEnabled;
    }

    public void setScheduleEnabled(boolean scheduleEnabled) {
        this.scheduleEnabled = scheduleEnabled;
    }

    public boolean isSurveyEnabled() {
        return surveyEnabled;
    }

    public void setSurveyEnabled(boolean surveyEnabled) {
        this.surveyEnabled = surveyEnabled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
