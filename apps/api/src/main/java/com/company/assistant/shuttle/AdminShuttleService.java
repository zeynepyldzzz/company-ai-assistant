package com.company.assistant.shuttle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminShuttleService {

    private final ShuttleRouteRepository shuttleRouteRepository;
    private final ShuttleStopRepository shuttleStopRepository;

    public AdminShuttleService(ShuttleRouteRepository shuttleRouteRepository,
            ShuttleStopRepository shuttleStopRepository) {
        this.shuttleRouteRepository = shuttleRouteRepository;
        this.shuttleStopRepository = shuttleStopRepository;
    }

    @Transactional
    public ShuttleRouteDetailResponse createRoute(ShuttleRouteRequest request) {
        ShuttleRoute route = new ShuttleRoute();
        route.setName(request.name());
        route.setPlateNumber(request.plateNumber());
        shuttleRouteRepository.save(route);

        List<ShuttleStop> stops = shuttleStopRepository.saveAll(toStops(request.stops(), route));
        return toDetailResponse(route, stops);
    }

    @Transactional
    public ShuttleRouteDetailResponse updateRoute(Integer routeId, ShuttleRouteRequest request) {
        ShuttleRoute route = shuttleRouteRepository.findById(routeId)
                .orElseThrow(() -> new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId));

        route.setName(request.name());
        if (request.plateNumber() != null) {
            route.setPlateNumber(request.plateNumber());
        }
        shuttleRouteRepository.save(route);

        shuttleStopRepository.deleteByRouteId(routeId);
        List<ShuttleStop> stops = shuttleStopRepository.saveAll(toStops(request.stops(), route));
        return toDetailResponse(route, stops);
    }

    @Transactional
    public ShuttleRoutePlateResponse updatePlate(Integer routeId, PlateUpdateRequest request) {
        ShuttleRoute route = shuttleRouteRepository.findById(routeId)
                .orElseThrow(() -> new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId));
        route.setPlateNumber(request.plateNumber());
        shuttleRouteRepository.save(route);
        return new ShuttleRoutePlateResponse(route.getId(), route.getPlateNumber());
    }

    private List<ShuttleStop> toStops(List<ShuttleStopRequest> requests, ShuttleRoute route) {
        return requests.stream().map(r -> {
            ShuttleStop stop = new ShuttleStop();
            stop.setRoute(route);
            stop.setName(r.name());
            stop.setTime(r.time());
            stop.setOrderIndex(r.orderIndex());
            stop.setLatitude(r.latitude());
            stop.setLongitude(r.longitude());
            return stop;
        }).toList();
    }

    private ShuttleRouteDetailResponse toDetailResponse(ShuttleRoute route, List<ShuttleStop> stops) {
        List<ShuttleStopResponse> stopResponses = stops.stream()
                .sorted(Comparator.comparingInt(ShuttleStop::getOrderIndex))
                .map(ShuttleStopResponse::new)
                .toList();
        return new ShuttleRouteDetailResponse(route.getId(), route.getName(), route.getPlateNumber(), stopResponses);
    }
}
