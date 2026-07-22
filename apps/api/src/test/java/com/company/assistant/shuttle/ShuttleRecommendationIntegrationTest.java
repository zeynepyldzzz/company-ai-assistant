package com.company.assistant.shuttle;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// B-BT2: GET /shuttle-routes/recommendation icin bilinen bir lat/lng ile en yakin
// durak/guzergah hesabinin gercek veritabanina (repository+servis+controller
// uctan uca) karsi dogru sonuc verdigini dogrular (FR-26, 27).
@SpringBootTest
@Transactional
class ShuttleRecommendationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ShuttleRouteRepository shuttleRouteRepository;

    @Autowired
    private ShuttleStopRepository shuttleStopRepository;

    private MockMvc mockMvc;

    private ShuttleStop insertStop(String routeName, String stopName, double lat, double lng) {
        ShuttleRoute route = new ShuttleRoute();
        route.setName(routeName);
        shuttleRouteRepository.saveAndFlush(route);

        ShuttleStop stop = new ShuttleStop();
        stop.setRoute(route);
        stop.setName(stopName);
        stop.setTime(LocalTime.of(8, 0));
        stop.setOrderIndex(1);
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        shuttleStopRepository.saveAndFlush(stop);
        return stop;
    }

    @Test
    @WithMockUser
    void bilinenKonumaEnYakinDurakVeGuzergahDogruDoner() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String token = UUID.randomUUID().toString().substring(0, 8);
        double stopLat = 39.1234;
        double stopLng = 32.5678;
        ShuttleStop stop = insertStop("Test Guzergahi " + token, "Test Duragi " + token, stopLat, stopLng);
        ShuttleRoute route = stop.getRoute();

        mockMvc.perform(get("/shuttle-routes/recommendation")
                        .param("lat", String.valueOf(stopLat))
                        .param("lng", String.valueOf(stopLng)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(route.getId()))
                .andExpect(jsonPath("$.routeName").value(route.getName()))
                .andExpect(jsonPath("$.stopId").value(stop.getId()))
                .andExpect(jsonPath("$.stopName").value(stop.getName()))
                .andExpect(jsonPath("$.distanceKm").value(closeTo(0.0, 0.01)))
                .andExpect(jsonPath("$.estimatedMinutes").value(0));
    }

    @Test
    @WithMockUser
    void ikiGuzergahArasindaCografiOlarakYakinOlanSecilir() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String token = UUID.randomUUID().toString().substring(0, 8);
        ShuttleStop yakinDurak = insertStop("Yakin Guzergah " + token, "Yakin Durak " + token, 39.9000, 32.8500);
        insertStop("Uzak Guzergah " + token, "Uzak Durak " + token, 41.0000, 29.0000);

        // Calisanin konumu "yakin" durağa cok yakin, "uzak" durağa binlerce km uzakta.
        mockMvc.perform(get("/shuttle-routes/recommendation")
                        .param("lat", "39.9005")
                        .param("lng", "32.8505"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeName").value(yakinDurak.getRoute().getName()))
                .andExpect(jsonPath("$.stopName").value(yakinDurak.getName()));
    }
}
