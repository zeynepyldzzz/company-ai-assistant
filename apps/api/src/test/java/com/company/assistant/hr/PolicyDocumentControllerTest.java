package com.company.assistant.hr;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyDocumentController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class PolicyDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PolicyDocumentService service;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    // /admin/** URL guard'i ROLE_ADMIN ister; method guard hr_admin/system_admin'e daraltir.
    private static SimpleGrantedAuthority[] hrAdmin() {
        return new SimpleGrantedAuthority[]{
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_HR_ADMIN")};
    }

    private static SimpleGrantedAuthority[] fleetAdmin() {
        return new SimpleGrantedAuthority[]{
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_FLEET_ADMIN")};
    }

    @Test
    void authOlmadan_401() throws Exception {
        mockMvc.perform(get("/admin/knowledge-base/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").exists());
    }

    @Test
    void employee_403() throws Exception {
        mockMvc.perform(get("/admin/knowledge-base/documents")
                        .with(user("emp").authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").exists());
    }

    // Admin ama hr_admin/system_admin degil -> method guard reddeder (issue kabul kriteri).
    @Test
    void adminAmaHrDegil_403() throws Exception {
        mockMvc.perform(get("/admin/knowledge-base/documents")
                        .with(user("fleet").authorities(fleetAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrAdmin_listeDoner_200() throws Exception {
        when(service.list(anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(ornekOzet()), 0, 20, 1));

        mockMvc.perform(get("/admin/knowledge-base/documents")
                        .with(user("hr").authorities(hrAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].title").value("İzin Prosedürü"));
    }

    @Test
    void hrAdmin_dokumanOlusturur_201() throws Exception {
        when(service.createDocument(any(), any())).thenReturn(ornekVersiyon());

        mockMvc.perform(post("/admin/knowledge-base/documents")
                        .with(user("hr").authorities(hrAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"procedureId":1,"title":"İzin Prosedürü",
                                 "content":"...","steps":[],"effectiveDate":"2026-02-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNo").value(1))
                .andExpect(jsonPath("$.isCurrent").value(true));
    }

    @Test
    void eksikTitle_400() throws Exception {
        mockMvc.perform(post("/admin/knowledge-base/documents")
                        .with(user("hr").authorities(hrAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"procedureId\":1,\"effectiveDate\":\"2026-02-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void olmayanDokumanSilme_404() throws Exception {
        doThrow(new PolicyDocumentNotFoundException("Dokuman bulunamadi: id=999"))
                .when(service).delete(eq(999));

        mockMvc.perform(delete("/admin/knowledge-base/documents/999")
                        .with(user("hr").authorities(hrAdmin())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("POLICY_DOCUMENT_NOT_FOUND"));
    }

    private DocumentSummary ornekOzet() {
        return new DocumentSummary(1, 2, "İzin Prosedürü", "izin", 1,
                LocalDate.of(2026, 2, 1), Instant.now());
    }

    private PolicyVersionResponse ornekVersiyon() {
        return new PolicyVersionResponse(10, 1, 1, "...", List.of(),
                LocalDate.of(2026, 2, 1), true, Instant.now(), 5);
    }
}
