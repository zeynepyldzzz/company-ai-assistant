package com.company.assistant.chatbot;

import java.time.Instant;
import java.util.List;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockitoBean
    private ChatWelcomeService chatWelcomeService;

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
                        "Bugünün menüsü: mercimek çorbası", "yemek_menusu", Instant.now(),
                        List.of(new ChatAction("menu", "Aylık menüyü gör")), List.of()));

        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"bugün yemekte ne var\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Bugünün menüsü: mercimek çorbası"))
                .andExpect(jsonPath("$.intent").value("yemek_menusu"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.actions[0].target").value("menu"))
                .andExpect(jsonPath("$.actions[0].label").value("Aylık menüyü gör"));
    }

    // A-22 (#141): hedef SEMANTIK olmali. Yanit govdesine web yolu sizarsa Faz 2'de mobil
    // istemci ayni yaniti kullanamaz — sozlesmenin en kolay bozulacak yeri burasi.
    @Test
    void aksiyonHedefi_webUrlIcermez() throws Exception {
        when(chatMessageService.handleMessage(any(), any()))
                .thenReturn(new ChatMessageResponse(
                        "Bilgi Teknolojileri departmanında ofiste görünenler: ...",
                        "calisma_duzeni", Instant.now(),
                        List.of(new ChatAction("directory_employees", "Tüm çalışanları gör")),
                        List.of()));

        mockMvc.perform(post("/chatbot/messages")
                .with(user("mustafa"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"kimler ofiste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions[0].target").value("directory_employees"))
                .andExpect(jsonPath("$.actions[0].target").value(not(containsString("/"))));
    }

    @Test
    void karsilama_metinVeOnerileriDoner() throws Exception {
        when(chatWelcomeService.buildWelcome(any()))
                .thenReturn(new ChatWelcomeResponse(
                        "Merhaba Mustafa! Sana şu konularda yardımcı olabilirim:",
                        List.of(new ChatSuggestion("Bugün yemekte ne var?", "Bugün yemekte ne var?"))));

        mockMvc.perform(get("/chatbot/welcome").with(user("mustafa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Merhaba Mustafa")))
                .andExpect(jsonPath("$.suggestions[0].question").value("Bugün yemekte ne var?"));
    }

    // Karsilama da korumali: anyRequest().authenticated() varsayilanina birakildi,
    // SecurityConfig'e satir eklenmedi. Bu test o varsayimi kilitler.
    @Test
    void karsilama_authOlmadan_401Doner() throws Exception {
        mockMvc.perform(get("/chatbot/welcome"))
                .andExpect(status().isUnauthorized());
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
