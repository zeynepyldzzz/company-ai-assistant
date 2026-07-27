package com.company.assistant.announcement;

import java.time.LocalDateTime;

/** GET /announcements, GET /announcements/{id}, GET /notifications cevabi. */
public record AnnouncementDto(Integer id, String title, String content, boolean pinned, LocalDateTime publishedAt) {

    public static AnnouncementDto from(Announcement announcement) {
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.isPinned(),
                announcement.getPublishedAt());
    }
}
