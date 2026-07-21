package com.company.assistant.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntentClassificationServiceTest {

    private JdbcTemplate jdbcTemplate;
    private EmbeddingClient embeddingClient;
    private IntentClassificationService service;

    private static final double THRESHOLD = 0.68;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        embeddingClient = mock(EmbeddingClient.class);
        service = new IntentClassificationService(jdbcTemplate, embeddingClient, THRESHOLD);
    }

    private void mockDbBestMatch(String intent, String phrase, double similarity) {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("name", intent, "phrase", phrase, "similarity", similarity)));
    }

    @Test
    void esikUstuBenzerlikteIntentDoner() {
        when(embeddingClient.embed("bugün yemekte ne var")).thenReturn(new float[]{0.1f, 0.2f});
        mockDbBestMatch("yemek_menusu", "bugün yemekte ne var", 0.85);

        var result = service.classify("bugün yemekte ne var");

        assertThat(result.matched()).isTrue();
        assertThat(result.intent()).isEqualTo("yemek_menusu");
        assertThat(result.similarity()).isEqualTo(0.85);
    }

    @Test
    void esikAltiBenzerlikteFallbackTetiklenir() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        mockDbBestMatch("duyurular", "yeni duyuru var mı", 0.45);

        var result = service.classify("bitcoin fiyatı ne kadar");

        assertThat(result.matched()).isFalse();
        assertThat(result.intent()).isEqualTo(IntentClassificationService.NO_INTENT);
    }

    @Test
    void tamEsikDegerindeIntentDoner() {
        // sinir durumu: similarity == threshold -> esik "altinda" degil, eslesme sayilir
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        mockDbBestMatch("anket", "aktif anket var mı", THRESHOLD);

        var result = service.classify("anket var mı");

        assertThat(result.matched()).isTrue();
    }

    @Test
    void embeddingServisiHataVerirseFallbackDoner() {
        when(embeddingClient.embed(anyString())).thenThrow(new IllegalStateException("Ollama kapali"));

        var result = service.classify("merhaba");

        assertThat(result.matched()).isFalse();
        assertThat(result.intent()).isEqualTo(IntentClassificationService.NO_INTENT);
    }

    @Test
    void ornekTablosuBossaFallbackDoner() {
        when(embeddingClient.embed(anyString())).thenReturn(new float[]{0.1f});
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        var result = service.classify("merhaba");

        assertThat(result.matched()).isFalse();
    }
}