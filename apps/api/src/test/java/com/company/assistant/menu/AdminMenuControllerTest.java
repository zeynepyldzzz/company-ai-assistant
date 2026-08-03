package com.company.assistant.menu;

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
import com.company.assistant.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-14 (#123): AdminMenuController artik yalnizca canteen_admin/system_admin ile
// erisilebiliyor (once genel hasRole("ADMIN") yeterliydi).
@WebMvcTest(AdminMenuController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminMenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MenuImportService menuImportService;

    @MockitoBean
    private MealMenuRepository mealMenuRepository;

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
    void canteenAdmin_menuSilebilir() throws Exception {
        when(mealMenuRepository.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/admin/menus/5")
                        .with(user("canteen").roles("ADMIN", "CANTEEN_ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void systemAdmin_deErisebilir() throws Exception {
        when(mealMenuRepository.existsById(5)).thenReturn(true);

        mockMvc.perform(delete("/admin/menus/5")
                        .with(user("sistem").roles("ADMIN", "SYSTEM_ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    // C-14 (#123): ROLE_ADMIN'e sahip (filter-chain seviyesini gecer) ama
    // canteen_admin/system_admin degil - method-level @PreAuthorize reddetmeli.
    @Test
    void baskaAdminAltRolu_403Doner() throws Exception {
        mockMvc.perform(delete("/admin/menus/5")
                        .with(user("hr").roles("ADMIN", "HR_ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(delete("/admin/menus/5").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void calisanRolu_403Doner() throws Exception {
        // ROLE_ADMIN'e bile sahip degil - filter-chain seviyesinde (SecurityConfig:
        // /admin/** hasRole(ADMIN)) reddedilmeli, @PreAuthorize'a hic ulasmamali.
        mockMvc.perform(delete("/admin/menus/5")
                        .with(user("calisan").roles("EMPLOYEE")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
