package com.company.assistant.shuttle;

import com.company.assistant.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShuttleService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    // MVP: gercek rota optimizasyonu yok (Faz 2). Sabit ortalama servis hizi
    // varsayimiyla kaba bir tahmini sure hesaplanir.
    private static final double AVERAGE_SPEED_KMH = 25.0;

    private final ShuttleRouteRepository shuttleRouteRepository;
    private final ShuttleStopRepository shuttleStopRepository;

    public ShuttleService(ShuttleRouteRepository shuttleRouteRepository,
            ShuttleStopRepository shuttleStopRepository) {
        this.shuttleRouteRepository = shuttleRouteRepository;
        this.shuttleStopRepository = shuttleStopRepository;
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

    public List<ShuttleStopResponse> getStops(Integer routeId) {
        if (!shuttleRouteRepository.existsById(routeId)) {
            throw new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId);
        }
        return shuttleStopRepository.findByRouteIdOrderByOrderIndexAsc(routeId)
                .stream().map(ShuttleStopResponse::new).toList();
    }

    public ShuttleRoutePlateResponse getPlate(Integer routeId) {
        ShuttleRoute route = shuttleRouteRepository.findById(routeId)
                .orElseThrow(() -> new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId));
        return new ShuttleRoutePlateResponse(route.getId(), route.getPlateNumber());
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

        int estimatedMinutes = (int) Math.round(nearestDistanceKm / AVERAGE_SPEED_KMH * 60);
        return new ShuttleRecommendationResponse(nearestStop, nearestDistanceKm, estimatedMinutes);
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
