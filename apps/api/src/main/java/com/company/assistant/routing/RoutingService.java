package com.company.assistant.routing;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * B-23: iki nokta arasindaki gercek yol mesafesi/suresi. Saglayici: OSRM (Open
 * Source Routing Machine), public demo sunucusu router.project-osrm.org.
 * Sunucu erisilemezse (network hatasi, timeout) cagiran taraf haversine
 * hesabina fallback yapmali - bu yuzden hata burada yutulup Optional.empty()
 * dondurulur, servis kesintisiz calismaya devam eder.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    // B-31: elle tiklanan noktalar piksel-hassasiyetinde olmayacagi icin
    // OSRM'in varsayilan 5m yaricapi cok siki - tum noktalara tek tip,
    // comert bir tolerans uygulanir.
    private static final int MATCH_RADIUS_METERS = 30;

    private final RestClient restClient;

    public RoutingService(@Value("${app.routing.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Optional<RouteResult> route(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            OsrmResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/{fromLng},{fromLat};{toLng},{toLat}")
                            .queryParam("overview", "false")
                            .build(fromLng, fromLat, toLng, toLat))
                    .retrieve()
                    .body(OsrmResponse.class);

            if (response == null || response.routes() == null || response.routes().isEmpty()) {
                return Optional.empty();
            }

            OsrmRoute route = response.routes().get(0);
            double distanceKm = route.distance() / 1000.0;
            int durationMinutes = (int) Math.round(route.duration() / 60.0);
            return Optional.of(new RouteResult(distanceKm, durationMinutes));
        } catch (RuntimeException e) {
            log.warn("OSRM rota sorgusu basarisiz, haversine fallback kullanilacak: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * B-28: bir guzergahin duraklari arasindaki gercek yol geometrisini (harita
     * cizgisi icin) OSRM'den tek istekte alir. OSRM erisilemezse veya rota
     * bulunamazsa Optional.empty() doner - cagiran taraf duraklarin duz cizgisine
     * fallback yapmali.
     */
    public Optional<List<Coordinate>> routeGeometry(List<Coordinate> waypoints) {
        if (waypoints.size() < 2) {
            return Optional.empty();
        }
        try {
            String coordinatesPath = waypoints.stream()
                    .map(c -> c.lng() + "," + c.lat())
                    .collect(Collectors.joining(";"));

            OsrmGeometryResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/" + coordinatesPath)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .build())
                    .retrieve()
                    .body(OsrmGeometryResponse.class);

            if (response == null || response.routes() == null || response.routes().isEmpty()) {
                return Optional.empty();
            }

            List<List<Double>> rawCoordinates = response.routes().get(0).geometry().coordinates();
            return Optional.of(rawCoordinates.stream()
                    .map(pair -> new Coordinate(pair.get(1), pair.get(0)))
                    .toList());
        } catch (RuntimeException e) {
            log.warn("OSRM rota geometrisi sorgusu basarisiz, duz cizgi fallback kullanilacak: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * B-31: admin haritada tikladigi ham noktalari OSRM'in Match servisine
     * (GPS/tiklama gurultusune tolerans gosteren, en yakin gercek yola
     * "snap" eden servis) gonderir. Noktalar yola makul sekilde oturmazsa
     * OSRM "NoMatch" donebilir - bu da basarisizlik sayilir.
     */
    public Optional<List<Coordinate>> matchGeometry(List<Coordinate> points) {
        if (points.size() < 2) {
            return Optional.empty();
        }
        try {
            String coordinatesPath = points.stream()
                    .map(c -> c.lng() + "," + c.lat())
                    .collect(Collectors.joining(";"));
            String radiuses = points.stream()
                    .map(c -> String.valueOf(MATCH_RADIUS_METERS))
                    .collect(Collectors.joining(";"));

            OsrmMatchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/match/v1/driving/" + coordinatesPath)
                            .queryParam("geometries", "geojson")
                            .queryParam("overview", "full")
                            .queryParam("radiuses", radiuses)
                            .build())
                    .retrieve()
                    .body(OsrmMatchResponse.class);

            if (response == null || !"Ok".equals(response.code())
                    || response.matchings() == null || response.matchings().isEmpty()) {
                return Optional.empty();
            }

            List<List<Double>> rawCoordinates = response.matchings().get(0).geometry().coordinates();
            return Optional.of(rawCoordinates.stream()
                    .map(pair -> new Coordinate(pair.get(1), pair.get(0)))
                    .toList());
        } catch (RuntimeException e) {
            log.warn("OSRM rota eslestirme (match) sorgusu basarisiz: {}", e.getMessage());
            return Optional.empty();
        }
    }

    record OsrmResponse(List<OsrmRoute> routes) {}

    record OsrmRoute(double distance, double duration) {}

    record OsrmGeometryResponse(List<OsrmGeometryRoute> routes) {}

    record OsrmGeometryRoute(OsrmGeometry geometry) {}

    record OsrmGeometry(List<List<Double>> coordinates) {}

    record OsrmMatchResponse(String code, List<OsrmMatching> matchings) {}

    record OsrmMatching(OsrmGeometry geometry) {}
}
