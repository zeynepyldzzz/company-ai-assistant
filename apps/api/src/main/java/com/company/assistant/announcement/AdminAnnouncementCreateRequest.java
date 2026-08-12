package com.company.assistant.announcement;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

/**
 * POST /admin/announcements govdesi. expiresAt opsiyoneldir (null = suresiz).
 *
 * <p>A-44 (#219): uzunluk sinirlari eklendi. {@code title} icin kaynak migration'daki kolon
 * genisligi (V1: {@code announcement.title VARCHAR(255)}); {@code content} kolonu TEXT oldugu
 * icin 2000 URUN karari.
 */
public record AdminAnnouncementCreateRequest(
        @Size(max = 255, message = "Başlık 255 karakteri aşamaz") String title,
        @Size(max = 2000, message = "Duyuru metni 2000 karakteri aşamaz") String content,
        LocalDateTime expiresAt) {
}
