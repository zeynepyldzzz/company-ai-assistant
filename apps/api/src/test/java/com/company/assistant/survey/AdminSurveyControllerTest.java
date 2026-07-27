package com.company.assistant.survey;

import java.time.LocalDateTime;
import java.util.Map;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-8 (#52): POST /admin/surveys, PUT /admin/surveys/{id}/publish, GET /admin/surveys/{id}/results.
// /admin/** SecurityConfig'te hasRole("ADMIN") ile korunuyor.
@WebMvcTest(AdminSurveyController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminSurveyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminSurveyService adminSurveyService;

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
    void admin_tumAnketleriListeleyebilir() throws Exception {
        when(adminSurveyService.listAll()).thenReturn(java.util.List.of(
                new AdminSurveyResponse(10, "Memnuniyet Anketi", true, LocalDateTime.of(2026, 7, 27, 10, 0)),
                new AdminSurveyResponse(11, "Taslak Anket", false, LocalDateTime.of(2026, 7, 26, 9, 0))));

        mockMvc.perform(get("/admin/surveys")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].published").value(false));
    }

    @Test
    void admin_anketOlusturabilir() throws Exception {
        when(adminSurveyService.createSurvey(eq(1), any())).thenReturn(
                new AdminSurveyResponse(10, "Memnuniyet Anketi", false, LocalDateTime.of(2026, 7, 27, 10, 0)));

        mockMvc.perform(post("/admin/surveys")
                        .with(user("1").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Memnuniyet Anketi\" }"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.published").value(false));

        verify(adminSurveyService).createSurvey(eq(1), any());
    }

    @Test
    void duzCalisan_anketOlusturmayaErisemez() throws Exception {
        mockMvc.perform(post("/admin/surveys")
                        .with(user("1").roles("EMPLOYEE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Memnuniyet Anketi\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(post("/admin/surveys")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Memnuniyet Anketi\" }"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_anketiYayimlayabilir() throws Exception {
        when(adminSurveyService.publish(10)).thenReturn(
                new AdminSurveyResponse(10, "Memnuniyet Anketi", true, LocalDateTime.of(2026, 7, 27, 10, 0)));

        mockMvc.perform(put("/admin/surveys/10/publish")
                        .with(user("1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void admin_olmayanAnketiYayimlarsa404Doner() throws Exception {
        when(adminSurveyService.publish(999)).thenThrow(new SurveyNotFoundException("Anket bulunamadı: 999"));

        mockMvc.perform(put("/admin/surveys/999/publish")
                        .with(user("1").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void admin_sonuclariOzetHalindeGorebilir() throws Exception {
        when(adminSurveyService.getResults(10)).thenReturn(new SurveyResultsResponse(
                10, "Memnuniyet Anketi", true, 2, 1,
                Map.of("q1", Map.of("evet", 2L)),
                java.util.List.of("harika bir anket")));

        mockMvc.perform(get("/admin/surveys/10/results")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResponses").value(2))
                .andExpect(jsonPath("$.totalFeedback").value(1))
                .andExpect(jsonPath("$.answerCounts.q1.evet").value(2));
    }
}
