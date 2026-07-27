package com.company.assistant.announcement;

/** GET/PUT /notifications/preferences cevabi. */
public record NotificationPreferenceResponse(boolean announcementEnabled, boolean scheduleEnabled, boolean surveyEnabled) {

    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isAnnouncementEnabled(),
                preference.isScheduleEnabled(),
                preference.isSurveyEnabled());
    }
}
