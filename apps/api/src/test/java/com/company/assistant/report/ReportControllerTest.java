package com.company.assistant.report;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.company.assistant.auth.JwtAuthFilter;
import com.company.assistant.auth.RestAccessDeniedHandler;
import com.company.assistant.auth.RestAuthenticationEntryPoint;
import com.company.assistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-11 (#85): GET /admin/reports/{type} ve export, yalnizca system_admin (FR-80-82).
@WebMvcTest(ReportController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private ReportExportService reportExportService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private static SimpleGrantedAuthority[] systemAdmin() {
        return new SimpleGrantedAuthority[]{
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN")};
    }

    private static SimpleGrantedAuthority[] hrAdmin() {
        return new SimpleGrantedAuthority[]{
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_HR_ADMIN")};
    }

    private static UsageReportResponse ornekRapor() {
        return new UsageReportResponse("usage", Instant.parse("2026-07-27T00:00:00Z"),
                List.of(new UsageReportRow("Chatbot", "Toplam Soru", 10)));
    }

    @Test
    void hrAdmin_raporaErisemez403() throws Exception {
        mockMvc.perform(get("/admin/reports/usage")
                        .with(user("hr").authorities(hrAdmin())).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdmin_usageRaporunuGorebilir() throws Exception {
        when(reportService.usageReport()).thenReturn(ornekRapor());

        mockMvc.perform(get("/admin/reports/usage")
                        .with(user("sys").authorities(systemAdmin())).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("usage"))
                .andExpect(jsonPath("$.rows[0].module").value("Chatbot"))
                .andExpect(jsonPath("$.rows[0].count").value(10));
    }

    @Test
    void bilinmeyenRaporTipi404Doner() throws Exception {
        mockMvc.perform(get("/admin/reports/gecersiz")
                        .with(user("sys").authorities(systemAdmin())).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void systemAdmin_xlsxDisaAktarabilir() throws Exception {
        when(reportService.usageReport()).thenReturn(ornekRapor());
        when(reportExportService.toXlsx(ornekRapor())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/admin/reports/usage/export").param("format", "xlsx")
                        .with(user("sys").authorities(systemAdmin())).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void desteklenmeyenFormat400Doner() throws Exception {
        when(reportService.usageReport()).thenReturn(ornekRapor());

        mockMvc.perform(get("/admin/reports/usage/export").param("format", "pdf")
                        .with(user("sys").authorities(systemAdmin())).with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
