package com.company.assistant.shuttle;

import com.company.assistant.geocoding.AddressNotFoundException;
import com.company.assistant.geocoding.AddressSuggestion;
import com.company.assistant.geocoding.GeocodingResult;
import com.company.assistant.geocoding.GeocodingService;
import com.company.assistant.routing.Coordinate;
import com.company.assistant.routing.RouteResult;
import com.company.assistant.routing.RoutingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// B-6: /shuttle-routes/recommendation - en yakin durak/guzergah ve tahmini sure hesabi (FR-26, 27)
// B-16: adres -> konum coz, ardindan ayni oneri mantigini calistir (FR-28)
// B-23: en yakin durak haversine ile on-elenir, gercek mesafe/sure OSRM'den alinir;
// OSRM erisilemezse haversine + sabit hiz fallback'ine dusulur (FR-27)
class ShuttleServiceTest {

    private ShuttleRouteRepository shuttleRouteRepository;
    private ShuttleStopRepository shuttleStopRepository;
    private ShuttleRoutePointRepository shuttleRoutePointRepository;
    private GeocodingService geocodingService;
    private RoutingService routingService;
    private ShuttleService service;

    @BeforeEach
    void setUp() {
        shuttleRouteRepository = mock(ShuttleRouteRepository.class);
        shuttleStopRepository = mock(ShuttleStopRepository.class);
        shuttleRoutePointRepository = mock(ShuttleRoutePointRepository.class);
        geocodingService = mock(GeocodingService.class);
        routingService = mock(RoutingService.class);
        when(routingService.route(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());
        service = new ShuttleService(shuttleRouteRepository, shuttleStopRepository, shuttleRoutePointRepository,
                geocodingService, routingService);
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
        assertThat(response.getSearchLat()).isEqualTo(40.9810);
        assertThat(response.getSearchLng()).isEqualTo(29.0310);
    }

    @Test
    void osrmErisilemezseTahminiSureSabitHizIleHesaplanir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Test Hatti");

        // ~25 km uzaklikta bir durak (yaklasik 1 derece enlem farki).
        ShuttleStop uzakDurak = stop(route, 9, "Uzak Durak", 41.0, 29.0);
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(uzakDurak));
        // setUp() varsayilan olarak Optional.empty() donduruyor (OSRM erisilemedi).

        ShuttleRecommendationResponse response = service.getRecommendation(40.775, 29.0);

        assertThat(response.getDistanceKm()).isCloseTo(25.0, org.assertj.core.data.Offset.offset(0.5));
        // 25 km / 25 km/s ortalama hiz varsayimi = ~60 dakika
        assertThat(response.getEstimatedMinutes()).isCloseTo(60, org.assertj.core.data.Offset.offset(2));
    }

    @Test
    void osrmBasariliysaGercekMesafeVeSureKullanilir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Test Hatti");

        // Haversine ~25 km olcer, ama gercek yol mesafesi/suresi (OSRM) daha uzun olabilir.
        ShuttleStop uzakDurak = stop(route, 9, "Uzak Durak", 41.0, 29.0);
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(uzakDurak));
        when(routingService.route(40.775, 29.0, 41.0, 29.0))
                .thenReturn(Optional.of(new RouteResult(31.4, 42)));

        ShuttleRecommendationResponse response = service.getRecommendation(40.775, 29.0);

        assertThat(response.getDistanceKm()).isEqualTo(31.4);
        assertThat(response.getEstimatedMinutes()).isEqualTo(42);
    }

    @Test
    void konumluDurakYoksaAnlamliHataFirlatir() {
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getRecommendation(40.98, 29.03))
                .isInstanceOf(NoShuttleRecommendationException.class);
    }

    @Test
    void adresGeocodeEdilipAyniOneriMantigiCalisir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        route.setName("Kadikoy Hatti");
        route.setPlateNumber("34 ABC 123");
        ShuttleStop nearestStop = stop(route, 1, "Kadikoy Merkez", 40.9800, 29.0300);
        when(shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull())
                .thenReturn(List.of(nearestStop));
        when(geocodingService.geocode("Kadıköy")).thenReturn(new GeocodingResult(40.9810, 29.0310));

        ShuttleRecommendationResponse response = service.getRecommendationByAddress("Kadıköy");

        assertThat(response.getStopId()).isEqualTo(1);
        assertThat(response.getRouteId()).isEqualTo(1);
        assertThat(response.getSearchLat()).isEqualTo(40.9810);
        assertThat(response.getSearchLng()).isEqualTo(29.0310);
    }

    @Test
    void adresOnerileriGeocodingServisindenIletilir() {
        List<AddressSuggestion> suggestions = List.of(
                new AddressSuggestion("Kadıköy, İstanbul", 40.9906, 29.0274),
                new AddressSuggestion("Kadıköy, İzmir", 38.4237, 27.1428));
        when(geocodingService.suggest("Kadıköy", 5)).thenReturn(suggestions);

        assertThat(service.getAddressSuggestions("Kadıköy")).isEqualTo(suggestions);
    }

    @Test
    void adresOnerileriBosSorguIcinGeocodingServisiCagrilmadanBosListeDoner() {
        assertThat(service.getAddressSuggestions("")).isEmpty();
        assertThat(service.getAddressSuggestions(null)).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(geocodingService);
    }

    @Test
    void taninmayanAdresGeocodingHatasiniIletir() {
        when(geocodingService.geocode("asdkjfhaskjdfh"))
                .thenThrow(new AddressNotFoundException("Adres bulunamadi: asdkjfhaskjdfh"));

        assertThatThrownBy(() -> service.getRecommendationByAddress("asdkjfhaskjdfh"))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void geometriOsrmBasariliysaGercekYolCizgisiKullanilir() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        ShuttleStop stop1 = stop(route, 1, "Merkez", 40.98, 29.03);
        ShuttleStop stop2 = stop(route, 2, "Sanayi", 40.99, 29.04);
        when(shuttleRouteRepository.existsById(1)).thenReturn(true);
        when(shuttleStopRepository.findByRouteIdOrderByOrderIndexAsc(1)).thenReturn(List.of(stop1, stop2));
        List<Coordinate> osrmGeometri = List.of(
                new Coordinate(40.98, 29.03), new Coordinate(40.985, 29.035), new Coordinate(40.99, 29.04));
        when(routingService.routeGeometry(List.of(new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04))))
                .thenReturn(Optional.of(osrmGeometri));

        ShuttleRouteGeometryResponse response = service.getRouteGeometry(1);

        assertThat(response.getRouteId()).isEqualTo(1);
        assertThat(response.getCoordinates()).isEqualTo(osrmGeometri);
    }

    @Test
    void geometriOsrmErisilemezseDurakDuzCizgisineFallbackYapar() {
        ShuttleRoute route = new ShuttleRoute();
        route.setId(1);
        ShuttleStop stop1 = stop(route, 1, "Merkez", 40.98, 29.03);
        ShuttleStop stop2 = stop(route, 2, "Sanayi", 40.99, 29.04);
        when(shuttleRouteRepository.existsById(1)).thenReturn(true);
        when(shuttleStopRepository.findByRouteIdOrderByOrderIndexAsc(1)).thenReturn(List.of(stop1, stop2));
        when(routingService.routeGeometry(anyList())).thenReturn(Optional.empty());

        ShuttleRouteGeometryResponse response = service.getRouteGeometry(1);

        assertThat(response.getCoordinates()).containsExactly(
                new Coordinate(40.98, 29.03), new Coordinate(40.99, 29.04));
    }

    @Test
    void geometriKayitliNoktalarVarsaDogrudanDonerOsrmSorulmaz() {
        when(shuttleRouteRepository.existsById(1)).thenReturn(true);
        ShuttleRoutePoint point1 = new ShuttleRoutePoint();
        point1.setLatitude(40.98);
        point1.setLongitude(29.03);
        point1.setOrderIndex(1);
        ShuttleRoutePoint point2 = new ShuttleRoutePoint();
        point2.setLatitude(40.985);
        point2.setLongitude(29.035);
        point2.setOrderIndex(2);
        when(shuttleRoutePointRepository.findByRouteIdOrderByOrderIndexAsc(1))
                .thenReturn(List.of(point1, point2));

        ShuttleRouteGeometryResponse response = service.getRouteGeometry(1);

        assertThat(response.getCoordinates()).containsExactly(
                new Coordinate(40.98, 29.03), new Coordinate(40.985, 29.035));
        org.mockito.Mockito.verifyNoInteractions(shuttleStopRepository);
        org.mockito.Mockito.verify(routingService, org.mockito.Mockito.never())
                .routeGeometry(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void geometriBulunamayanRota404Firlatir() {
        when(shuttleRouteRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> service.getRouteGeometry(999))
                .isInstanceOf(ShuttleRouteNotFoundException.class);
    }
}
