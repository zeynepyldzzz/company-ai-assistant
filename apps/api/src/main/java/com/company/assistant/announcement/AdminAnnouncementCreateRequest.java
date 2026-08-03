package com.company.assistant.announcement;

import java.time.LocalDateTime;

/** POST /admin/announcements govdesi. expiresAt opsiyoneldir (null = suresiz). */
public record AdminAnnouncementCreateRequest(String title, String content, LocalDateTime expiresAt) {
}
