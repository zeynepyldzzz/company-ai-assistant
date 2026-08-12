package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * PUT /admin/surveys/{id} govdesi.
 * C-13 (#121): admin baslik, secenekler ve gecerlilik (deadline) tarihini duzenleyebilir.
 */
public record AdminSurveyUpdateRequest(
        // A-44 (#219): sinirlar create ile AYNI olmali; ikisi ayrisirsa duzenleme yoluyla
        // olusturmada engellenen veri yazilabilir.
        @Size(max = 255, message = "Anket başlığı 255 karakteri aşamaz") String title,
        LocalDateTime deadline,
        List<@Size(max = 255, message = "Seçenek metni 255 karakteri aşamaz") String> options) {
}
