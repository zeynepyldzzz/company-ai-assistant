package com.company.assistant.announcement;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Size;

/** PUT /admin/announcements/{id} govdesi. expiresAt opsiyoneldir (null = suresiz). */
public record AdminAnnouncementUpdateRequest(
        // A-44 (#219): sinirlar create ile AYNI olmali; ikisi ayrisirsa duzenleme yoluyla
        // olusturmada engellenen veri yazilabilir.
        @Size(max = 255, message = "Başlık 255 karakteri aşamaz")
        String title,
        @Size(max = 2000, message = "Duyuru metni 2000 karakteri aşamaz")
        String content,
        LocalDateTime expiresAt) {
}
