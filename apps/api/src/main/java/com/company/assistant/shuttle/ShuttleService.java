package com.company.assistant.shuttle;

import com.company.assistant.common.PagedResponse;
import com.company.assistant.geocoding.GeocodingResult;
import com.company.assistant.geocoding.GeocodingService;
import com.company.assistant.routing.Coordinate;
import com.company.assistant.routing.RouteResult;
import com.company.assistant.routing.RoutingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShuttleService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    // MVP: gercek rota optimizasyonu yok (Faz 2). Sabit ortalama servis hizi
    // varsayimiyla kaba bir tahmini sure hesaplanir.
    private static final double AVERAGE_SPEED_KMH = 25.0;

    private final ShuttleRouteRepository shuttleRouteRepository;
    private final ShuttleStopRepository shuttleStopRepository;
    private final GeocodingService geocodingService;
    private final RoutingService routingService;

    public ShuttleService(ShuttleRouteRepository shuttleRouteRepository,
            ShuttleStopRepository shuttleStopRepository,
            GeocodingService geocodingService,
            RoutingService routingService) {
        this.shuttleRouteRepository = shuttleRouteRepository;
        this.shuttleStopRepository = shuttleStopRepository;
        this.geocodingService = geocodingService;
        this.routingService = routingService;
    }

    public PagedResponse<ShuttleRouteResponse> listRoutes(int page, int pageSize) {
        Page<ShuttleRoute> result = shuttleRouteRepository.findAll(PageRequest.of(page, pageSize));
        return new PagedResponse<>(
                result.getContent().stream().map(ShuttleRouteResponse::new).toList(),
                page,
                pageSize,
                result.getTotalElements()
        );
    }

    /**
     * A-12: chatbot icin sayfalamasiz guzergah listesi. listRoutes() HTTP kaygisi olan
     * PagedResponse dondurur; chatbot (ve Faz 2'de LLM tool'u) ham listeyi ister.
     */
    public List<ShuttleRouteResponse> getAllRoutes() {
        return shuttleRouteRepository.findAll().stream().map(ShuttleRouteResponse::new).toList();
    }

    /**
     * A-18 (#127): birden fazla guzergahin duraklari tek sorguda, route id'ye gore gruplu.
     * Chatbot resolver'i tum hatlari birden gosterdigi icin hat basina sorgu atmak
     * gereksiz (9 guzergahta mesaj basina 19 sorgu ediyordu).
     */
    public Map<Integer, List<ShuttleStopResponse>> getStopsByRoutes(Collection<Integer> routeIds) {
        if (routeIds.isEmpty()) {
            return Map.of();
        }
        return shuttleStopRepository.findByRouteIds(routeIds).stream()
                .collect(Collectors.groupingBy(
                        stop -> stop.getRoute().getId(),
                        Collectors.mapping(ShuttleStopResponse::new, Collectors.toList())));
    }

    public List<ShuttleStopResponse> getStops(Integer routeId) {
        if (!shuttleRouteRepository.existsById(routeId)) {
            throw new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId);
        }
        return shuttleStopRepository.findByRouteIdOrderByOrderIndexAsc(routeId)
                .stream().map(ShuttleStopResponse::new).toList();
    }

    /**
     * B-28: bir guzergahin duraklari arasindaki gercek yol geometrisi (harita
     * cizgisi icin). OSRM erisilemezse veya rota bulunamazsa duraklarin kendi
     * konumlarina (duz cizgi) fallback yapilir.
     */
    public ShuttleRouteGeometryResponse getRouteGeometry(Integer routeId) {
        if (!shuttleRouteRepository.existsById(routeId)) {
            throw new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId);
        }

        List<Coordinate> waypoints = shuttleStopRepository.findByRouteIdOrderByOrderIndexAsc(routeId).stream()
                .filter(stop -> stop.getLatitude() != null && stop.getLongitude() != null)
                .map(stop -> new Coordinate(stop.getLatitude(), stop.getLongitude()))
                .toList();

        List<Coordinate> coordinates = routingService.routeGeometry(waypoints).orElse(waypoints);
        return new ShuttleRouteGeometryResponse(routeId, coordinates);
    }

    public ShuttleRoutePlateResponse getPlate(Integer routeId) {
        ShuttleRoute route = shuttleRouteRepository.findById(routeId)
                .orElseThrow(() -> new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId));
        return new ShuttleRoutePlateResponse(route.getId(), route.getName(), route.getPlateNumber());
    }

    public ShuttleRecommendationResponse getRecommendationByAddress(String address) {
        GeocodingResult location = geocodingService.geocode(address);
        return getRecommendation(location.lat(), location.lng());
    }

    public ShuttleRecommendationResponse getRecommendation(double lat, double lng) {
        List<ShuttleStop> stops = shuttleStopRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull();
        if (stops.isEmpty()) {
            throw new NoShuttleRecommendationException(
                    "Konum bilgisi bulunan bir durak yok, oneri yapilamiyor");
        }

        ShuttleStop nearestStop = null;
        double nearestDistanceKm = Double.MAX_VALUE;
        for (ShuttleStop stop : stops) {
            double distanceKm = haversineKm(lat, lng, stop.getLatitude(), stop.getLongitude());
            if (distanceKm < nearestDistanceKm) {
                nearestDistanceKm = distanceKm;
                nearestStop = stop;
            }
        }

        double distanceKm = nearestDistanceKm;
        int estimatedMinutes = (int) Math.round(nearestDistanceKm / AVERAGE_SPEED_KMH * 60);

        Optional<RouteResult> route = routingService.route(
                lat, lng, nearestStop.getLatitude(), nearestStop.getLongitude());
        if (route.isPresent()) {
            distanceKm = route.get().distanceKm();
            estimatedMinutes = route.get().durationMinutes();
        }

        return new ShuttleRecommendationResponse(nearestStop, distanceKm, estimatedMinutes);
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
