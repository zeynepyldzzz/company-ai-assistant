package com.company.assistant.announcement;

import java.time.LocalDateTime;

/** GET /announcements, GET /announcements/{id}, GET /notifications cevabi. */
public record AnnouncementDto(Integer id, String title, String content, boolean pinned, LocalDateTime publishedAt,
                               LocalDateTime expiresAt, boolean active) {

    public static AnnouncementDto from(Announcement announcement) {
        LocalDateTime expiresAt = announcement.getExpiresAt();
        boolean active = expiresAt == null || expiresAt.isAfter(LocalDateTime.now());
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPinned(),
                announcement.getPublishedAt(),
                expiresAt,
                active);
    }
}
