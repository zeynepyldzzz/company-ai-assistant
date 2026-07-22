package com.company.assistant.shuttle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// B-6: /shuttle-routes/recommendation - en yakin durak/guzergah ve tahmini sure hesabi (FR-26, 27)
class ShuttleServiceTest {

    private ShuttleRouteRepository shuttleRouteRepository;
    private ShuttleStopRepository shuttleStopRepository;
    private ShuttleService service;

    @BeforeEach
    void setUp() {
        shuttleRouteRepository = mock(ShuttleRouteRepository.class);
        shuttleStopRepository = mock(ShuttleStopRepository.class);
        service = new ShuttleService(shuttleRouteRepository, shuttleStopRepository);
    }

    private ShuttleStop stop(ShuttleRoute route, Integer id, String name, double lat, double lng) {
        ShuttleStop stop = new ShuttleStop();
        stop.setId(id);
        stop.setRoute(route);
        stop.setName(name);
        stop.setTime(LocalTime.of(8, 0));
        stop.setOrderIndex(1);
        stop.setLatitude(lat);
        stop.setLongitude(lng);
        return stop;
    }

    @Test
    void enYakinDurakVeGuzergahDoguHesaplanir() {
        ShuttleRoute kadikoy = new ShuttleRoute();
        kadikoy.setId(1);
        kadikoy.setName("Kadikoy Hatti");
        kadikoy.setPlateNumber("34 ABC 123");

        ShuttleRoute bornova = new ShuttleRoute();
        bornova.setId(2);
        bornova.setName("Bornova Hatti");
        bornova.setPlateNumber("35 XYZ 999");

        // Calisan konumu: Kadikoy merkeze cok yakin, Bornova durakli guzergahtan uzak.
        ShuttleStop kadikoyMerkez = stop(kadikoy, 1, "Kadikoy Merkez", 40.9800, 29.0300);
        ShuttleStop bornovaMerkez = stop(bornova, 2, "Bornova Merkez", 38.4600, 27.2200);
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(bornovaMerkez, kadikoyMerkez));

        ShuttleRecommendationResponse response = service.getRecommendation(40.9810, 29.0310);

        assertThat(response.getStopId()).isEqualTo(1);
        assertThat(response.getStopName()).isEqualTo("Kadikoy Merkez");
        assertThat(response.getRouteId()).isEqualTo(1);
        assertThat(response.getRouteName()).isEqualTo("Kadikoy Hatti");
        assertThat(response.getPlateNumber()).isEqualTo("34 ABC 123");
    }

    @Test
    void tahminiSureBasitSabitHizIleHesaplanir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Test Hatti");

        // ~25 km uzaklikta bir durak (yaklasik 1 derece enlem farki).
        ShuttleStop uzakDurak = stop(route, 9, "Uzak Durak", 41.0, 29.0);
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(uzakDurak));

        ShuttleRecommendationResponse response = service.getRecommendation(40.775, 29.0);

        assertThat(response.getDistanceKm()).isCloseTo(25.0, org.assertj.core.data.Offset.offset(0.5));
        // 25 km / 25 km/s ortalama hiz varsayimi = ~60 dakika
        assertThat(response.getEstimatedMinutes()).isCloseTo(60, org.assertj.core.data.Offset.offset(2));
    }

    @Test
    void konumluDurakYoksaAnlamliHataFirlatir() {
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getRecommendation(40.98, 29.03))
                .isInstanceOf(NoShuttleRecommendationException.class);
    }
}
