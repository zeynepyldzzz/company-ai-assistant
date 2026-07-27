package com.company.assistant.announcement;

/** PUT /notifications/preferences govdesi. FR-65-67. */
public record NotificationPreferenceRequest(boolean announcementEnabled, boolean scheduleEnabled, boolean surveyEnabled) {
}
