package com.company.assistant.announcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-18 (#127): duyuru listesi. Odak: sabitlenen isaretlenmesi, veri yoklugu ve liste kesme.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementVariableResolverTest {

    @Mock
    private AnnouncementService announcementService;

    private AnnouncementVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AnnouncementVariableResolver(announcementService);
    }

    @Test
    void duyuruDisiIntentBosDoner() {
        assertThat(resolver.resolve("yemek_menusu")).isEmpty();
    }

    @Test
    void duyuruYoksaNetMesajDoner() {
        when(announcementService.getAnnouncements()).thenReturn(List.of());

        assertThat(resolver.resolve("duyurular").get("duyurular"))
                .isEqualTo("Şu anda yayında duyuru bulunmuyor.");
    }

    @Test
    void sabitlenenDuyuruIsaretlenir() {
        when(announcementService.getAnnouncements()).thenReturn(List.of(
                announcement("Yıllık izin takvimi", true),
                announcement("Kantin saatleri değişti", false)));

        String reply = resolver.resolve("duyurular").get("duyurular");

        assertThat(reply)
                .contains("• Yıllık izin takvimi (sabitlenmiş)")
                .contains("• Kantin saatleri değişti")
                .doesNotContain("Kantin saatleri değişti (sabitlenmiş)");
    }

    @Test
    void uzunListeKesilipKalanSayiBelirtilir() {
        List<AnnouncementDto> many = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> announcement("Duyuru " + i, false))
                .toList();
        when(announcementService.getAnnouncements()).thenReturn(many);

        String reply = resolver.resolve("duyurular").get("duyurular");

        assertThat(reply).contains("Duyuru 5").doesNotContain("Duyuru 6");
        assertThat(reply).contains("ve 3 duyuru daha");
    }

    // Yayin tarihi NULL olabilir; ham "null" kullaniciya gorunmemeli.
    @Test
    void tarihiOlmayanDuyuruNullBasmaz() {
        when(announcementService.getAnnouncements())
                .thenReturn(List.of(new AnnouncementDto(1, "Tarihsiz duyuru", "govde", false, null)));

        assertThat(resolver.resolve("duyurular").get("duyurular")).doesNotContain("null");
    }

    // Duyuru govdesi uzun olabilir; chatbot yanitinda yalnizca baslik gecmeli.
    @Test
    void duyuruGovdesiYanittaYerAlmaz() {
        when(announcementService.getAnnouncements()).thenReturn(List.of(
                new AnnouncementDto(1, "Kısa başlık", "Çok uzun duyuru gövdesi metni", false,
                        LocalDateTime.of(2026, 7, 20, 9, 0))));

        assertThat(resolver.resolve("duyurular").get("duyurular"))
                .contains("Kısa başlık")
                .doesNotContain("Çok uzun duyuru gövdesi metni");
    }

    private AnnouncementDto announcement(String title, boolean pinned) {
        return new AnnouncementDto(1, title, "govde", pinned, LocalDateTime.of(2026, 7, 20, 9, 0));
    }
}
