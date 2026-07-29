package com.company.assistant.announcement;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * A-18: 'duyurular' intent'i icin canli duyuru listesi (FR-45/47).
 *
 * Uretilen anahtar V28 template'iyle eslesir:
 *   {{duyurular}} -> sabitlenenler ustte olacak sekilde baslik + tarih listesi
 *
 * Yalnizca BASLIK ve tarih basilir, duyuru govdesi degil: govde uzun olabilir ve chatbot
 * balonunu kullanilmaz hale getirir. Detay icin kullanici Duyurular ekranina gider.
 * AnnouncementService.getAnnouncements() sabitlenenleri zaten one aliyor, ek siralama yok.
 */
@Component
public class AnnouncementVariableResolver {

    private static final String ANNOUNCEMENT_INTENT = "duyurular";
    private static final String VARIABLE = "duyurular";

    private static final String NO_ANNOUNCEMENT = "Şu anda yayında duyuru bulunmuyor.";
    private static final int MAX_ITEMS = 5;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", new Locale("tr"));

    private final AnnouncementService announcementService;

    public AnnouncementVariableResolver(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    public Map<String, String> resolve(String intentName) {
        if (!ANNOUNCEMENT_INTENT.equals(intentName)) {
            return Map.of();
        }

        List<AnnouncementDto> announcements = announcementService.getAnnouncements();
        if (announcements.isEmpty()) {
            return Map.of(VARIABLE, NO_ANNOUNCEMENT);
        }

        String body = announcements.stream()
                .limit(MAX_ITEMS)
                .map(this::formatLine)
                .collect(Collectors.joining("\n"));

        int remaining = announcements.size() - Math.min(announcements.size(), MAX_ITEMS);
        String more = remaining > 0 ? "\n• ve " + remaining + " duyuru daha" : "";

        return Map.of(VARIABLE, "Güncel duyurular:\n" + body + more);
    }

    private String formatLine(AnnouncementDto announcement) {
        String pinned = announcement.pinned() ? " (sabitlenmiş)" : "";
        String date = announcement.publishedAt() != null
                ? " — " + announcement.publishedAt().format(DATE_FMT)
                : "";
        return "• " + announcement.title() + pinned + date;
    }
}
