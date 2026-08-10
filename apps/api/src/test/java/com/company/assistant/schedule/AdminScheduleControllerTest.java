package com.company.assistant.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

/**
 * A-36 (#200): GET /admin/schedules rol daraltmasi.
 *
 * <p>Bu uc onceden yalnizca SecurityConfig'in {@code /admin/**} kuraliyla korunuyordu, yani
 * HER admin alt rolu tum calisanlarin haftalik planini gorebiliyordu. Diger sekiz yonetim
 * ucunun hepsinde rol daraltmasi vardi; bu tek istisnaydi ve calisan verisi tasidigi icin
 * hr_admin + system_admin'e daraltildi.
 *
 * <p>Testin asil isi daraltmanin GERI ALINMAMASI: anotasyon silinirse fleet_admin dali
 * 200 doner ve burasi kirilir.
 */
@WebMvcTest(AdminScheduleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminScheduleService adminScheduleService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private static SimpleGrantedAuthority[] admin(String subRole) {
        return new SimpleGrantedAuthority[]{
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority(subRole)};
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(get("/admin/schedules"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void calisan_403Doner() throws Exception {
        mockMvc.perform(get("/admin/schedules")
                        .with(user("emp").authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
    }

    // A-36'nin getirdigi kisit: admin olmak yetmiyor, DOGRU alt rol gerekiyor.
    @Test
    void fleetAdmin_403Doner() throws Exception {
        mockMvc.perform(get("/admin/schedules")
                        .with(user("fleet").authorities(admin("ROLE_FLEET_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void canteenAdmin_403Doner() throws Exception {
        mockMvc.perform(get("/admin/schedules")
                        .with(user("canteen").authorities(admin("ROLE_CANTEEN_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void hrAdmin_200Doner() throws Exception {
        when(adminScheduleService.getAllForCurrentWeek())
                .thenReturn(new AdminScheduleResponse(LocalDate.of(2026, 8, 10), List.of()));

        mockMvc.perform(get("/admin/schedules")
                        .with(user("hr").authorities(admin("ROLE_HR_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void systemAdmin_200Doner() throws Exception {
        when(adminScheduleService.getAllForCurrentWeek())
                .thenReturn(new AdminScheduleResponse(LocalDate.of(2026, 8, 10), List.of()));

        mockMvc.perform(get("/admin/schedules")
                        .with(user("sys").authorities(admin("ROLE_SYSTEM_ADMIN"))))
                .andExpect(status().isOk());
    }
}
