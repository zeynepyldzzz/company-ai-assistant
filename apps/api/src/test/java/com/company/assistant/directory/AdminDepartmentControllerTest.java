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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// #84 (Hafta 4): POST/PUT/DELETE /admin/departments (FR-68-71).
@WebMvcTest(AdminDepartmentController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminDepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDepartmentService adminDepartmentService;

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

    private static final String GECERLI_GOVDE = "{ \"name\": \"Muhasebe\" }";

    @Test
    void adminAmaHrDegil_403() throws Exception {
        mockMvc.perform(post("/admin/departments")
                        .with(user("fleet").authorities(fleetAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrAdmin_departmanOlusturabilir() throws Exception {
        Department saved = new Department();
        saved.setId(5);
        saved.setName("Muhasebe");
        when(adminDepartmentService.create(any())).thenReturn(new DepartmentResponse(saved));

        mockMvc.perform(post("/admin/departments")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GECERLI_GOVDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Muhasebe"));
    }

    @Test
    void hrAdmin_departmanGuncelleyebilir() throws Exception {
        Department saved = new Department();
        saved.setId(5);
        saved.setName("Muhasebe ve Finans");
        when(adminDepartmentService.update(anyInt(), any())).thenReturn(new DepartmentResponse(saved));

        mockMvc.perform(put("/admin/departments/5")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Muhasebe ve Finans\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Muhasebe ve Finans"));
    }

    @Test
    void hrAdmin_departmaniSilebilir() throws Exception {
        mockMvc.perform(delete("/admin/departments/5")
                        .with(user("hr").authorities(hrAdmin())).with(csrf()))
                .andExpect(status().isNoContent());

        verify(adminDepartmentService).delete(5);
    }

    // FR-71: calisan atanmis departman silinemez (409).
    @Test
    void calisaniOlanDepartmanSilinirse409Doner() throws Exception {
        doThrow(new DepartmentInUseException("Departmana atanmış çalışanlar olduğu için silinemez: 5"))
                .when(adminDepartmentService).delete(5);

        mockMvc.perform(delete("/admin/departments/5")
                        .with(user("hr").authorities(hrAdmin())).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void olmayanDepartmanSilinirse404Doner() throws Exception {
        doThrow(new DepartmentNotFoundException("Departman bulunamadı: 999"))
                .when(adminDepartmentService).delete(999);

        mockMvc.perform(delete("/admin/departments/999")
                        .with(user("hr").authorities(hrAdmin())).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
