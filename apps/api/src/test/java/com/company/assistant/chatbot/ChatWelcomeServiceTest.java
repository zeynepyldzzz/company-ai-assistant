package com.company.assistant.chatbot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * A-22 (#141): karsilama. Odak, issue'nun tek gercek is kurali — metnin AYRI bir yerde
 * tutulmamasi. Ikinci bir kopya, ilkinin eskimesi demektir; A-20'de selamlama metninin
 * aylar once eklenen yeteneklerden habersiz kaldigini gorduk.
 */
@ExtendWith(MockitoExtension.class)
class ChatWelcomeServiceTest {

    @Mock
    private TemplateResponseService templateResponseService;
    @Mock
    private ChatVariableResolver variableResolver;
    @Mock
    private IntentSuggestionRepository suggestionRepository;

    @InjectMocks
    private ChatWelcomeService chatWelcomeService;

    @Test
    void karsilamaMetniSelamlamaTemplateindenUretilir() {
        when(variableResolver.resolve(any())).thenReturn(Map.of("kullanici_adi", "Mustafa"));
        when(templateResponseService.buildResponse(eq("selamlama"), any()))
                .thenReturn("Merhaba Mustafa! Sana şu konularda yardımcı olabilirim:");

        var welcome = chatWelcomeService.buildWelcome(null);

        assertThat(welcome.message()).isEqualTo("Merhaba Mustafa! Sana şu konularda yardımcı olabilirim:");
        verify(templateResponseService).buildResponse(eq("selamlama"), any());
    }

    @Test
    void onerilerIntentTablosundanGelir() {
        when(variableResolver.resolve(any())).thenReturn(Map.of());
        when(templateResponseService.buildResponse(any(), any())).thenReturn("Merhaba!");
        when(suggestionRepository.findSuggestions()).thenReturn(List.of(
                new ChatSuggestion("Bugün yemekte ne var?", "Bugün yemekte ne var?"),
                new ChatSuggestion("Servisim kaçta?", "Servisim kaçta?")));

        var welcome = chatWelcomeService.buildWelcome(null);

        assertThat(welcome.suggestions())
                .extracting(ChatSuggestion::question)
                .containsExactly("Bugün yemekte ne var?", "Servisim kaçta?");
    }
}
