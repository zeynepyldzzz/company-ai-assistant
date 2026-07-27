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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-9 (#53): POST /admin/announcements, PUT /admin/announcements/{id}/pin.
@WebMvcTest(AdminAnnouncementController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminAnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAnnouncementService adminAnnouncementService;

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
    void admin_duyuruOlusturabilir() throws Exception {
        when(adminAnnouncementService.create(eq(1), any())).thenReturn(
                new AnnouncementDto(10, "Baslik", "icerik", false, LocalDateTime.of(2026, 7, 27, 10, 0)));

        mockMvc.perform(post("/admin/announcements")
                        .with(user("1").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Baslik\", \"content\": \"icerik\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.pinned").value(false));

        verify(adminAnnouncementService).create(eq(1), any());
    }

    @Test
    void duzCalisan_duyuruOlusturmayaErisemez() throws Exception {
        mockMvc.perform(post("/admin/announcements")
                        .with(user("1").roles("EMPLOYEE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Baslik\", \"content\": \"icerik\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_duyuruyuSabitleyebilir() throws Exception {
        when(adminAnnouncementService.togglePin(10)).thenReturn(
                new AnnouncementDto(10, "Baslik", "icerik", true, LocalDateTime.of(2026, 7, 27, 10, 0)));

        mockMvc.perform(put("/admin/announcements/10/pin")
                        .with(user("1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void olmayanDuyuruSabitlenirse404Doner() throws Exception {
        when(adminAnnouncementService.togglePin(999)).thenThrow(new AnnouncementNotFoundException("Duyuru bulunamadı: 999"));

        mockMvc.perform(put("/admin/announcements/999/pin")
                        .with(user("1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
