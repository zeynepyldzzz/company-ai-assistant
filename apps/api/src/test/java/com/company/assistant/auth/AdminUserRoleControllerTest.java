package com.company.assistant.auth;

import java.util.Optional;

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

import com.company.assistant.config.SecurityConfig;
import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-11 (#85): PUT /admin/users/{id}/roles yalnizca system_admin (FR-80-82).
@WebMvcTest(AdminUserRoleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private RoleRepository roleRepository;

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

    @Test
    void hrAdmin_rolAtayamaz403() throws Exception {
        mockMvc.perform(put("/admin/users/1/roles")
                        .with(user("hr").authorities(hrAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"roleId\": 2 }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void systemAdmin_rolAtayabilir() throws Exception {
        Employee employee = new Employee();
        employee.setId(1);
        employee.setName("Test Calisan");
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));

        Role role = new Role();
        role.setId(2);
        role.setName("fleet_admin");
        when(roleRepository.findById(2)).thenReturn(Optional.of(role));

        mockMvc.perform(put("/admin/users/1/roles")
                        .with(user("sys").authorities(systemAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"roleId\": 2 }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.roleId").value(2))
                .andExpect(jsonPath("$.roleName").value("fleet_admin"));
    }

    @Test
    void olmayanKullanici404Doner() throws Exception {
        when(employeeRepository.findById(anyInt())).thenReturn(Optional.empty());

        mockMvc.perform(put("/admin/users/999/roles")
                        .with(user("sys").authorities(systemAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"roleId\": 2 }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void olmayanRol400Doner() throws Exception {
        Employee employee = new Employee();
        employee.setId(1);
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(roleRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(put("/admin/users/1/roles")
                        .with(user("sys").authorities(systemAdmin())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"roleId\": 999 }"))
                .andExpect(status().isBadRequest());
    }
}
