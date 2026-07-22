package com.company.assistant.hr;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.company.assistant.auth.JwtAuthFilter;
import com.company.assistant.auth.RestAccessDeniedHandler;
import com.company.assistant.auth.RestAuthenticationEntryPoint;
import com.company.assistant.common.PagedResponse;
import com.company.assistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HrProcedureController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class HrProcedureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HrProcedureService service;

    // SecurityConfig JwtAuthFilter istiyor; pass-through stub (ChatbotControllerTest deseni).
    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private HrProcedureDetail ornekDetay() {
        return new HrProcedureDetail(1, "İşe Giriş ve Oryantasyon Prosedürü", "onboarding",
                "İnsan Kaynakları", "ik@sirket.com", 1, LocalDate.of(2026, 1, 1),
                "İçerik", List.of(new ProcedureStep(1, "Evrak teslimi", "İlk gün...")));
    }

    @Test
    void topicYok_sayfaliListeZarfiDoner() throws Exception {
        var ozet = new HrProcedureSummary(1, "İşe Giriş ve Oryantasyon Prosedürü", "onboarding",
                "İnsan Kaynakları", "ik@sirket.com", 1, LocalDate.of(2026, 1, 1));
        when(service.list(anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(ozet), 0, 20, 1));

        mockMvc.perform(get("/hr/procedures").with(user("mustafa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].category").value("onboarding"))
                .andExpect(jsonPath("$.data[0].title").value("İşe Giriş ve Oryantasyon Prosedürü"));
    }

    @Test
    void topicVar_tekilNesneDoner() throws Exception {
        when(service.getByTopic(eq("onboarding"))).thenReturn(ornekDetay());

        mockMvc.perform(get("/hr/procedures").param("topic", "onboarding").with(user("mustafa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("İşe Giriş ve Oryantasyon Prosedürü"))
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.steps[0].title").value("Evrak teslimi"));
    }

    @Test
    void gecersizTopic_404VeStandartHataFormati() throws Exception {
        when(service.getByTopic(eq("gecersiz")))
                .thenThrow(new HrProcedureNotFoundException("Prosedur bulunamadi (topic=gecersiz)"));

        mockMvc.perform(get("/hr/procedures").param("topic", "gecersiz").with(user("mustafa")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HR_PROCEDURE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    void authOlmadan_401VeStandartHataFormati() throws Exception {
        mockMvc.perform(get("/hr/procedures"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").exists())
                .andExpect(jsonPath("$.error.message").exists());
    }
}
