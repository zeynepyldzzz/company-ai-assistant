package com.company.assistant.announcement;

import java.time.LocalDateTime;

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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-9 (#53): GET /announcements, GET /announcements/{id}, GET /notifications, PUT /notifications/preferences.
@WebMvcTest(AnnouncementController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementService announcementService;

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
    void girisYapmisCalisan_duyurulariGorebilir() throws Exception {
        when(announcementService.getAnnouncements()).thenReturn(java.util.List.of(
                new AnnouncementDto(1, "Sabitli Duyuru", "icerik", true, LocalDateTime.of(2026, 7, 20, 9, 0)),
                new AnnouncementDto(2, "Yeni Duyuru", "icerik2", false, LocalDateTime.of(2026, 7, 27, 9, 0))));

        mockMvc.perform(get("/announcements")
                        .with(user("1").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void authOlmadan_duyurulariGoremez() throws Exception {
        mockMvc.perform(get("/announcements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tekDuyuruBulunamazsa404Doner() throws Exception {
        when(announcementService.getAnnouncement(99)).thenThrow(new AnnouncementNotFoundException("Duyuru bulunamadı: 99"));

        mockMvc.perform(get("/announcements/99")
                        .with(user("1").roles("EMPLOYEE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void calisan_bildirimTercihleriniGuncelleyebilir() throws Exception {
        when(announcementService.updatePreferences(eq(1), any()))
                .thenReturn(new NotificationPreferenceResponse(false, true, true));

        mockMvc.perform(put("/notifications/preferences")
                        .with(user("1").roles("EMPLOYEE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"announcementEnabled\": false, \"scheduleEnabled\": true, \"surveyEnabled\": true }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcementEnabled").value(false));

        verify(announcementService).updatePreferences(eq(1), any());
    }

    @Test
    void calisan_kendiBildirimlerini_JWTdenAlinanKimlikleGorur() throws Exception {
        when(announcementService.getNotifications(1)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/notifications")
                        .with(user("1").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(announcementService).getNotifications(1);
    }
}
