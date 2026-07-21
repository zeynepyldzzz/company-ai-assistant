package com.company.assistant.chatbot;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateResponseServiceTest {

    private ResponseTemplateRepository repository;
    private TemplateResponseService service;

    @BeforeEach
    void setUp() {
        repository = mock(ResponseTemplateRepository.class);
        // Renderer bilincli olarak gercek: saf/deterministik bir sinif, mock'lamak
        // sadece test kodunu sisirir. Boylece secim + render zinciri birlikte dogrulanir.
        service = new TemplateResponseService(repository, new TemplateRenderer());
    }

    @Test
    void intentTemplateVarsaOnuKullanir() {
        when(repository.findEnabledTemplateByIntentName("yemek_menusu"))
                .thenReturn(Optional.of("Bugunun menusu: {{menu}}"));

        var yanit = service.buildResponse("yemek_menusu", Map.of("menu", "mercimek corbasi"));

        assertThat(yanit).isEqualTo("Bugunun menusu: mercimek corbasi");
    }

    @Test
    void intentTemplateYoksaFallbackIntenteDuser() {
        when(repository.findEnabledTemplateByIntentName("bilinmeyen_intent"))
                .thenReturn(Optional.empty());
        when(repository.findEnabledTemplateByIntentName("intent_bulunamadi"))
                .thenReturn(Optional.of("Sorunu anlayamadim {{kullanici_adi}}."));

        var yanit = service.buildResponse("bilinmeyen_intent", Map.of("kullanici_adi", "Mustafa"));

        assertThat(yanit).isEqualTo("Sorunu anlayamadim Mustafa.");
    }

    @Test
    void fallbackTemplateDeYoksaSonCareYanitDoner() {
        when(repository.findEnabledTemplateByIntentName("bilinmeyen_intent"))
                .thenReturn(Optional.empty());
        when(repository.findEnabledTemplateByIntentName("intent_bulunamadi"))
                .thenReturn(Optional.empty());

        var yanit = service.buildResponse("bilinmeyen_intent", Map.of());

        assertThat(yanit).startsWith("Üzgünüm");
    }
}