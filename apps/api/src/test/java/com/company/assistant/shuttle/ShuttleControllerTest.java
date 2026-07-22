package com.company.assistant.shuttle;

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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-5: GET /shuttle-routes, /{id}/stops, /{id}/plate (FR-22-25) - herhangi bir calisan erisebilir.
@WebMvcTest(ShuttleController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class ShuttleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShuttleService shuttleService;

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
    void listRoutes_dondurur() throws Exception {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Kadikoy Hatti");
        route.setPlateNumber("34 ABC 123");
        PagedResponse<ShuttleRouteResponse> response = new PagedResponse<>(
                List.of(new ShuttleRouteResponse(route)), 0, 20, 1);
        when(shuttleService.listRoutes(0, 20)).thenReturn(response);

        mockMvc.perform(get("/shuttle-routes").with(user("calisan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Kadikoy Hatti"))
                .andExpect(jsonPath("$.data[0].plateNumber").value("34 ABC 123"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void getStops_siraliDoner() throws Exception {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        ShuttleStop stop1 = stop(route, "Merkez", LocalTime.of(8, 0), 1);
        ShuttleStop stop2 = stop(route, "Sanayi", LocalTime.of(8, 15), 2);
        when(shuttleService.getStops(1))
                .thenReturn(List.of(new ShuttleStopResponse(stop1), new ShuttleStopResponse(stop2)));

        mockMvc.perform(get("/shuttle-routes/1/stops").with(user("calisan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Merkez"))
                .andExpect(jsonPath("$[1].name").value("Sanayi"));
    }

    @Test
    void getStops_bulunamayanRota404Doner() throws Exception {
        when(shuttleService.getStops(999))
                .thenThrow(new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: 999"));

        mockMvc.perform(get("/shuttle-routes/999/stops").with(user("calisan")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SHUTTLE_ROUTE_NOT_FOUND"));
    }

    @Test
    void getPlate_dondurur() throws Exception {
        when(shuttleService.getPlate(1)).thenReturn(new ShuttleRoutePlateResponse(1, "34 ABC 123"));

        mockMvc.perform(get("/shuttle-routes/1/plate").with(user("calisan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plateNumber").value("34 ABC 123"));
    }

    @Test
    void authOlmadan_401Doner() throws Exception {
        mockMvc.perform(get("/shuttle-routes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void getRecommendation_enYakinDurakVeGuzergahDoner() throws Exception {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Kadikoy Hatti");
        route.setPlateNumber("34 ABC 123");
        ShuttleStop nearestStop = stop(route, "Merkez", LocalTime.of(8, 0), 1);
        nearestStop.setId(5);
        nearestStop.setLatitude(40.99);
        nearestStop.setLongitude(29.02);

        when(shuttleService.getRecommendation(40.98, 29.03))
                .thenReturn(new ShuttleRecommendationResponse(nearestStop, 1.5, 4));

        mockMvc.perform(get("/shuttle-routes/recommendation")
                        .param("lat", "40.98")
                        .param("lng", "29.03")
                        .with(user("calisan")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(1))
                .andExpect(jsonPath("$.routeName").value("Kadikoy Hatti"))
                .andExpect(jsonPath("$.stopId").value(5))
                .andExpect(jsonPath("$.stopName").value("Merkez"))
                .andExpect(jsonPath("$.distanceKm").value(1.5))
                .andExpect(jsonPath("$.estimatedMinutes").value(4));
    }

    @Test
    void getRecommendation_konumluDurakYoksa404Doner() throws Exception {
        when(shuttleService.getRecommendation(40.98, 29.03))
                .thenThrow(new NoShuttleRecommendationException(
                        "Konum bilgisi bulunan bir durak yok, oneri yapilamiyor"));

        mockMvc.perform(get("/shuttle-routes/recommendation")
                        .param("lat", "40.98")
                        .param("lng", "29.03")
                        .with(user("calisan")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_SHUTTLE_RECOMMENDATION_AVAILABLE"));
    }

    private ShuttleStop stop(ShuttleRoute route, String name, LocalTime time, int orderIndex) {
        ShuttleStop stop = new ShuttleStop();
        stop.setRoute(route);
        stop.setName(name);
        stop.setTime(time);
        stop.setOrderIndex(orderIndex);
        return stop;
    }
}
