package com.company.assistant.survey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-18 (#127): aktif anket listesi. Yayimlanmamis anket filtresi SurveyService'te (C-8/#52);
 * buradaki odak sunum ve kapsam sinirlari.
 */
@ExtendWith(MockitoExtension.class)
class SurveyVariableResolverTest {

    @Mock
    private SurveyService surveyService;

    private SurveyVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SurveyVariableResolver(surveyService);
    }

    @Test
    void anketDisiIntentBosDoner() {
        assertThat(resolver.resolve("duyurular")).isEmpty();
    }

    @Test
    void aktifAnketYoksaNetMesajDoner() {
        when(surveyService.getActiveSurveys()).thenReturn(List.of());

        assertThat(resolver.resolve("anket").get("aktif_anketler"))
                .isEqualTo("Şu anda katılabileceğin aktif bir anket görünmüyor.");
    }

    @Test
    void aktifAnketlerBasliklariylaListelenir() {
        when(surveyService.getActiveSurveys()).thenReturn(List.of(
                new SurveyDto(1, "Çalışan memnuniyeti", LocalDateTime.of(2026, 7, 20, 9, 0)),
                new SurveyDto(2, "Kantin anketi", LocalDateTime.of(2026, 7, 22, 9, 0))));

        String reply = resolver.resolve("anket").get("aktif_anketler");

        assertThat(reply)
                .contains("• Çalışan memnuniyeti — 20.07.2026")
                .contains("• Kantin anketi")
                .contains("Anketler bölümünden katılabilirsin");
    }

    // FR-44: anket sonuclari yetkili kullaniciya ait; chatbot katilim sayisi/sonuc basmaz.
    @Test
    void sonucVeyaKatilimSayisiYanittaYerAlmaz() {
        when(surveyService.getActiveSurveys())
                .thenReturn(List.of(new SurveyDto(1, "Çalışan memnuniyeti", LocalDateTime.now())));

        String reply = resolver.resolve("anket").get("aktif_anketler");

        assertThat(reply).doesNotContain("sonuç").doesNotContain("katılım sayısı");
    }

    @Test
    void tarihiOlmayanAnketNullBasmaz() {
        when(surveyService.getActiveSurveys()).thenReturn(List.of(new SurveyDto(1, "Tarihsiz", null)));

        assertThat(resolver.resolve("anket").get("aktif_anketler")).doesNotContain("null");
    }
}
