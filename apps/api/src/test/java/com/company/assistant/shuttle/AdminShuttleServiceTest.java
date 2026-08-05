package com.company.assistant.shuttle;

import com.company.assistant.routing.Coordinate;
import com.company.assistant.routing.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// B-31: guzergah olustururken/guncellerken haritada cizilip OSRM Match ile
// eslesmis rota noktalarinin (opsiyonel) durak listesiyle ayni sekilde
// (delete-and-recreate) persist edilmesi, ve match/geometry-points uclarinin
// servis katmani.
class AdminShuttleServiceTest {

    private ShuttleRouteRepository shuttleRouteRepository;
    private ShuttleStopRepository shuttleStopRepository;
    private ShuttleRoutePointRepository shuttleRoutePointRepository;
    private RoutingService routingService;
    private AdminShuttleService service;

    @BeforeEach
    void setUp() {
        shuttleRouteRepository = mock(ShuttleRouteRepository.class);
        shuttleStopRepository = mock(ShuttleStopRepository.class);
        shuttleRoutePointRepository = mock(ShuttleRoutePointRepository.class);
        routingService = mock(RoutingService.class);
        service = new AdminShuttleService(
                shuttleRouteRepository, shuttleStopRepository, shuttleRoutePointRepository, routingService);

        when(shuttleStopRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(shuttleRoutePointRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ShuttleRouteRequest request(List<RoutePointRequest> geometryPoints) {
        return new ShuttleRouteRequest("Kadikoy Hatti", "34 ABC 123", null, null,
                List.of(new ShuttleStopRequest("Merkez", java.time.LocalTime.of(8, 0), 1, null, null)),
                geometryPoints);
    }

    @Test
    void olusturma_geometriNoktalariVarsaSiraliOlarakPersistEdilir() {
        List<RoutePointRequest> points = List.of(
                new RoutePointRequest(40.98, 29.03), new RoutePointRequest(40.99, 29.04));

        service.createRoute(request(points));

        org.mockito.ArgumentCaptor<List<ShuttleRoutePoint>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(shuttleRoutePointRepository).saveAll(captor.capture());
        List<ShuttleRoutePoint> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getOrderIndex()).isEqualTo(1);
        assertThat(saved.get(0).getLatitude()).isEqualTo(40.98);
        assertThat(saved.get(1).getOrderIndex()).isEqualTo(2);
    }

    @Test
    void olusturma_geometriNoktalariYoksaPersistEdilmez() {
        service.createRoute(request(null));

        verify(shuttleRoutePointRepository, never()).saveAll(anyList());
    }

    @Test
    void guncelleme_eskiGeometriSilinipYenisiKaydedilir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        when(shuttleRouteRepository.findById(1)).thenReturn(Optional.of(route));

        service.updateRoute(1, request(List.of(new RoutePointRequest(40.98, 29.03))));

        verify(shuttleRoutePointRepository).deleteByRouteId(1);
        verify(shuttleRoutePointRepository).saveAll(anyList());
    }

    @Test
    void eslestirme_basariliysaKoordinatlariDoner() {
        when(routingService.matchGeometry(anyList())).thenReturn(Optional.of(
                List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04))));

        RouteMatchResponse response = service.matchRouteGeometry(
                new RouteMatchRequest(List.of(new RoutePointRequest(40.98, 29.03), new RoutePointRequest(40.99, 29.04))));

        assertThat(response.coordinates()).hasSize(2);
    }

    @Test
    void eslestirme_basarisizsaHataFirlatir() {
        when(routingService.matchGeometry(anyList())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.matchRouteGeometry(
                new RouteMatchRequest(List.of(new RoutePointRequest(40.98, 29.03), new RoutePointRequest(0.0, 0.0)))))
                .isInstanceOf(RouteMatchFailedException.class);
    }

    @Test
    void geometriNoktalariGetir_bulunamayanRota404Firlatir() {
        when(shuttleRouteRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.getGeometryPoints(999))
                .isInstanceOf(ShuttleRouteNotFoundException.class);
    }

    @Test
    void geometriNoktalariGetir_kayitliNoktalariDoner() {
        when(shuttleRouteRepository.existsById(1)).thenReturn(true);
        ShuttleRoutePoint point = new ShuttleRoutePoint();
        point.setLatitude(40.98);
        point.setLongitude(29.03);
        when(shuttleRoutePointRepository.findByRouteIdOrderByOrderIndexAsc(1)).thenReturn(List.of(point));

        List<Coordinate> result = service.getGeometryPoints(1);

        assertThat(result).containsExactly(new Coordinate(40.98, 29.03));
    }
}
