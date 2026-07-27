package com.company.assistant.schedule;

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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-T2 (#33) Test #1: FR-63 sahiplik.
// Kimlik HER ZAMAN JWT'den (authentication.getName()) alinir, istek govdesinden degil.
// Not: /schedules/{id} gibi baska bir calisana dokunan bir uc YOK; sadece /me var.
// Bu yuzden "baskasinin verisini degistirme" tasarim geregi imkansiz.
@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduleService scheduleService;

    // SecurityConfig JwtAuthFilter istiyor; pass-through stub (diger test dosyalarindaki desen).
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    // Bu controller getName()'i Integer'a ceviriyor; o yuzden giris kullanicisi SAYISAL olmali.
    private static final String GOVDE = "{ \"weekStartDate\": \"2026-07-20\", \"days\": [] }";

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void putMe_kimlik42_servis42IleCagrilir() throws Exception {
        mockMvc.perform(put("/schedules/me")
                        .with(user("42")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVDE))
                .andExpect(status().isOk());

        // Ispat: govde ne olursa olsun, servis token'daki 42 ile cagrildi.
        verify(scheduleService).saveMySchedule(eq(42), any());
    }

    @Test
    void putMe_ayniGovdeKimlik99_servis99IleCagrilir() throws Exception {
        mockMvc.perform(put("/schedules/me")
                        .with(user("99")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVDE))
                .andExpect(status().isOk());

        // Ayni govde, farkli giris -> servis 99 ile cagrildi.
        // Demek ki kimlik govdeden degil, token'dan geliyor (FR-63).
        verify(scheduleService).saveMySchedule(eq(99), any());
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(put("/schedules/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GOVDE))
                .andExpect(status().isUnauthorized());
    }
}