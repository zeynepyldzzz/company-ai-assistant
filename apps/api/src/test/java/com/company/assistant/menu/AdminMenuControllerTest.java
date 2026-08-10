package com.company.assistant.menu;

import jakarta.servlet.FilterChain;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    private MealItemRepository mealItemRepository;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    private static final String UPDATE_REQUEST_JSON = """
            {
              "items": [
                {"category": "CORBA", "name": "Mercimek Çorbası"},
                {"category": "ANA_YEMEK", "name": "Tavuk Sote"}
              ]
            }
            """;

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

    // #194: PUT /admin/menus/{id} - gunun kalemlerini elle duzenleme.

    @Test
    void canteenAdmin_menuGuncelleyebilir() throws Exception {
        MealMenu menu = new MealMenu();
        menu.setId(5);
        menu.setDate(LocalDate.of(2026, 8, 10));
        when(mealMenuRepository.findById(5)).thenReturn(Optional.of(menu));
        when(mealItemRepository.saveAll(any())).thenReturn(List.of());

        mockMvc.perform(put("/admin/menus/5")
                        .with(user("canteen").roles("ADMIN", "CANTEEN_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void systemAdmin_menuGuncelleyebilir() throws Exception {
        MealMenu menu = new MealMenu();
        menu.setId(5);
        menu.setDate(LocalDate.of(2026, 8, 10));
        when(mealMenuRepository.findById(5)).thenReturn(Optional.of(menu));
        when(mealItemRepository.saveAll(any())).thenReturn(List.of());

        mockMvc.perform(put("/admin/menus/5")
                        .with(user("sistem").roles("ADMIN", "SYSTEM_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void baskaAdminAltRolu_guncellemeye403Doner() throws Exception {
        mockMvc.perform(put("/admin/menus/5")
                        .with(user("hr").roles("ADMIN", "HR_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void authOlmadan_guncellemeye401Doner() throws Exception {
        mockMvc.perform(put("/admin/menus/5").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void calisanRolu_guncellemeye403Doner() throws Exception {
        mockMvc.perform(put("/admin/menus/5")
                        .with(user("calisan").roles("EMPLOYEE")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void bosKalemListesi_400ValidationHatasiDoner() throws Exception {
        mockMvc.perform(put("/admin/menus/5")
                        .with(user("canteen").roles("ADMIN", "CANTEEN_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void bosIsim_400ValidationHatasiDoner() throws Exception {
        mockMvc.perform(put("/admin/menus/5")
                        .with(user("canteen").roles("ADMIN", "CANTEEN_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items": [{"category": "CORBA", "name": ""}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void olmayanMenu_404Doner() throws Exception {
        when(mealMenuRepository.findById(eq(99))).thenReturn(Optional.empty());

        mockMvc.perform(put("/admin/menus/99")
                        .with(user("canteen").roles("ADMIN", "CANTEEN_ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
