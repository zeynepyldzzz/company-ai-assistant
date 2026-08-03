package com.company.assistant.announcement;

import java.time.LocalDateTime;

/** PUT /admin/announcements/{id} govdesi. expiresAt opsiyoneldir (null = suresiz). */
public record AdminAnnouncementUpdateRequest(String title, String content, LocalDateTime expiresAt) {
}
