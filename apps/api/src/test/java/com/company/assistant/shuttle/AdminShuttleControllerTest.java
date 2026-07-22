package com.company.assistant.shuttle;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-5: POST/PUT /admin/shuttle-routes, PUT /admin/shuttle-routes/{id}/plate (FR-73)
// Yalnizca shuttle_admin / system_admin erisebilir.
@WebMvcTest(AdminShuttleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminShuttleControllerTest {

    private static final String ROUTE_REQUEST_JSON = """
            {
              "name": "Kadikoy Hatti",
              "plateNumber": "34 ABC 123",
              "stops": [
                {"name": "Merkez", "time": "08:00:00", "orderIndex": 1},
                {"name": "Sanayi", "time": "08:15:00", "orderIndex": 2}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminShuttleService adminShuttleService;

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
    void shuttleAdmin_yeniRotaOlusturabilir() throws Exception {
        ShuttleStopResponse stop1 = new ShuttleStopResponse(stop("Merkez", 1));
        ShuttleStopResponse stop2 = new ShuttleStopResponse(stop("Sanayi", 2));
        when(adminShuttleService.createRoute(any())).thenReturn(
                new ShuttleRouteDetailResponse(1, "Kadikoy Hatti", "34 ABC 123", List.of(stop1, stop2)));

        mockMvc.perform(post("/admin/shuttle-routes")
                        .with(user("koordinator").roles("ADMIN", "SHUTTLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Kadikoy Hatti"))
                .andExpect(jsonPath("$.stops.length()").value(2));
    }

    @Test
    void systemAdmin_deErisebilir() throws Exception {
        when(adminShuttleService.createRoute(any())).thenReturn(
                new ShuttleRouteDetailResponse(1, "Kadikoy Hatti", "34 ABC 123", List.of()));

        mockMvc.perform(post("/admin/shuttle-routes")
                        .with(user("sistem").roles("ADMIN", "SYSTEM_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isCreated());
    }

    // B-BT2: shuttle_admin/system_admin disindaki tum admin alt rolleri, her 3
    // admin ucunda da 403 almali. Bu kullanicilar ROLE_ADMIN'e sahip (filter-chain
    // seviyesini gecer) ama shuttle_admin/system_admin degil - method-level
    // @PreAuthorize reddetmeli.
    @ParameterizedTest
    @ValueSource(strings = {"HR_ADMIN", "FLEET_ADMIN", "CANTEEN_ADMIN"})
    void digerAdminAltRolleri_createRotaya403Alir(String subRole) throws Exception {
        mockMvc.perform(post("/admin/shuttle-routes")
                        .with(user("test").roles("ADMIN", subRole))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR_ADMIN", "FLEET_ADMIN", "CANTEEN_ADMIN"})
    void digerAdminAltRolleri_updateRotaya403Alir(String subRole) throws Exception {
        mockMvc.perform(put("/admin/shuttle-routes/1")
                        .with(user("test").roles("ADMIN", subRole))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HR_ADMIN", "FLEET_ADMIN", "CANTEEN_ADMIN"})
    void digerAdminAltRolleri_plakaGuncellemeye403Alir(String subRole) throws Exception {
        mockMvc.perform(put("/admin/shuttle-routes/1/plate")
                        .with(user("test").roles("ADMIN", subRole))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\": \"34 XYZ 999\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    // Duz calisan (hicbir admin rolu yok) filter-chain seviyesinde reddedilir -
    // method-level @PreAuthorize'a hic ulasmadan farkli bir mekanizmayla 403 doner.
    @Test
    void duzCalisan_adminUcunaFilterSeviyesinde403Alir() throws Exception {
        mockMvc.perform(post("/admin/shuttle-routes")
                        .with(user("calisan").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(post("/admin/shuttle-routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adSiz_422ValidationHatasiDoner() throws Exception {
        mockMvc.perform(post("/admin/shuttle-routes")
                        .with(user("koordinator").roles("ADMIN", "SHUTTLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "stops": [{"name": "Merkez", "time": "08:00:00", "orderIndex": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shuttleAdmin_rotaGuncelleyebilir() throws Exception {
        when(adminShuttleService.updateRoute(eq(1), any())).thenReturn(
                new ShuttleRouteDetailResponse(1, "Kadikoy Hatti - Guncel", "34 ABC 123", List.of()));

        mockMvc.perform(put("/admin/shuttle-routes/1")
                        .with(user("koordinator").roles("ADMIN", "SHUTTLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Kadikoy Hatti - Guncel"));
    }

    @Test
    void shuttleAdmin_bulunamayanRotaGuncellerse404Doner() throws Exception {
        when(adminShuttleService.updateRoute(eq(999), any()))
                .thenThrow(new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: 999"));

        mockMvc.perform(put("/admin/shuttle-routes/999")
                        .with(user("koordinator").roles("ADMIN", "SHUTTLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROUTE_REQUEST_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SHUTTLE_ROUTE_NOT_FOUND"));
    }

    @Test
    void shuttleAdmin_plakaGuncelleyebilir() throws Exception {
        when(adminShuttleService.updatePlate(eq(1), any()))
                .thenReturn(new ShuttleRoutePlateResponse(1, "34 XYZ 999"));

        mockMvc.perform(put("/admin/shuttle-routes/1/plate")
                        .with(user("koordinator").roles("ADMIN", "SHUTTLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plateNumber\": \"34 XYZ 999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("34 XYZ 999"));
    }

    private ShuttleStop stop(String name, int orderIndex) {
        ShuttleStop stop = new ShuttleStop();
        stop.setName(name);
        stop.setOrderIndex(orderIndex);
        return stop;
    }
}
