package com.company.assistant.chatbot;

import java.time.Instant;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.company.assistant.auth.JwtAuthFilter;
import com.company.assistant.auth.RestAccessDeniedHandler;
import com.company.assistant.auth.RestAuthenticationEntryPoint;
import com.company.assistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatbotController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ChatbotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatMessageService chatMessageService;

    // SecurityConfig constructor'i JwtAuthFilter istiyor; mock veriyoruz ama
    // mock filtre zinciri ilerletmez, istek controller'a hic ulasmaz.
    // O yuzden pass-through stub'liyoruz: gelen istegi aynen devam ettir.
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void happyPath_soruGonderilir_yanitDoner() throws Exception {
        when(chatMessageService.handleMessage(eq("bugün yemekte ne var"), any()))
                .thenReturn(new ChatMessageResponse(
                        "Bugünün menüsü: mercimek çorbası", "yemek_menusu", Instant.now()));

        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"bugün yemekte ne var\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Bugünün menüsü: mercimek çorbası"))
                .andExpect(jsonPath("$.intent").value("yemek_menusu"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void bosMesaj_400VeValidationErrorDoner() throws Exception {
        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Mesaj boş olamaz"));
    }

    @Test
    void binKarakterUstuMesaj_400Doner() throws Exception {
        String uzunMesaj = "a".repeat(1001);
        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"" + uzunMesaj + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").value("Mesaj 1000 karakteri aşamaz"));
    }

    @Test
    void bozukJsonGovde_400Doner() throws Exception {
        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("bu json degil"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void authOlmadan_401VeStandartHataFormatiDoner() throws Exception {
        mockMvc.perform(post("/chatbot/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"merhaba\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").exists())
                .andExpect(jsonPath("$.error.message").exists());
    }
}
