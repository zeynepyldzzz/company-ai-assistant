package com.company.assistant.directory;

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
import com.company.assistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #84 (Hafta 4): POST/PUT/DELETE /admin/employees (FR-68-71). /admin/** URL guard'i
// ROLE_ADMIN ister; method guard hr_admin/system_admin'e daraltir (A-6 deseni).
@WebMvcTest(AdminEmployeeController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminEmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminEmployeeService adminEmployeeService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

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

    // A-30 (#185): departman artik zorunlu — gecerli govde onsuz olamaz. Bu sabitin adi
    // "gecerli" oldugu icin eksik kalirsa RBAC testleri de 400 alip yanlis nedenle patliyordu.
    private static final String GECERLI_GOVDE =
            "{ \"name\": \"Ayşe Yılmaz\", \"email\": \"ayse@company.com\", \"departmentId\": 3 }";

    @Test
    void authOlmadan_401() throws Exception {
        mockMvc.perform(post("/admin/employees").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duzCalisan_403() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .with(user("emp").authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isForbidden());
    }

    // Admin ama hr_admin/system_admin degil -> method guard reddeder.
    @Test
    void adminAmaHrDegil_403() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .with(user("fleet").authorities(fleetAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrAdmin_calisanOlusturabilir() throws Exception {
        Employee saved = new Employee();
        saved.setId(10);
        saved.setName("Ayşe Yılmaz");
        saved.setEmail("ayse@company.com");
        // A-29 (#178): yanit artik EmployeeResponse degil; sifre alani listeleme/detay
        // uclarindan sizmasin diye olusturma icin ayri bir tip kullaniliyor.
        when(adminEmployeeService.create(any()))
                .thenReturn(new AdminEmployeeCreateResponse(new EmployeeResponse(saved), "Gecici123!"));

        mockMvc.perform(post("/admin/employees")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employee.id").value(10))
                .andExpect(jsonPath("$.employee.name").value("Ayşe Yılmaz"))
                .andExpect(jsonPath("$.generatedPassword").value("Gecici123!"));
    }

    @Test
    void hrAdmin_bosIsimleValidasyonHatasiAlir() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\", \"email\": \"ayse@company.com\" }"))
                .andExpect(status().isBadRequest());
    }

    // A-30 (#185): departmansiz calisan olusturulamaz. Kisitlama yalnizca uygulama
    // katmaninda (kolonda NOT NULL yok, mevcut satirlarda NULL var) — dolayisiyla bu
    // testin kalkmasi kurali sessizce iptal eder.
    @Test
    void hrAdmin_departmansizCalisanOlusturamaz() throws Exception {
        mockMvc.perform(post("/admin/employees")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Ayşe Yılmaz\", \"email\": \"ayse@company.com\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hrAdmin_calisanGuncelleyebilir() throws Exception {
        Employee saved = new Employee();
        saved.setId(10);
        saved.setName("Ayşe Yılmaz Güncel");
        saved.setEmail("ayse@company.com");
        when(adminEmployeeService.update(anyInt(), any())).thenReturn(new EmployeeResponse(saved));

        mockMvc.perform(put("/admin/employees/10")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Ayşe Yılmaz Güncel\", \"email\": \"ayse@company.com\", \"departmentId\": 3 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ayşe Yılmaz Güncel"));
    }

    @Test
    void olmayanCalisanGuncellenirse404Doner() throws Exception {
        doThrow(new EmployeeNotFoundException("Çalışan bulunamadı: 999"))
                .when(adminEmployeeService).update(anyInt(), any());

        mockMvc.perform(put("/admin/employees/999")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isNotFound());
    }

    @Test
    void hrAdmin_calisanSilebilir() throws Exception {
        mockMvc.perform(delete("/admin/employees/10")
                        .with(user("hr").authorities(hrAdmin())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(adminEmployeeService).delete(10);
    }

    @Test
    void duzCalisan_silmeyeErisemez() throws Exception {
        mockMvc.perform(delete("/admin/employees/10")
                        .with(user("emp").authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
