package com.company.assistant.shuttle;

import com.company.assistant.routing.Coordinate;
import com.company.assistant.routing.RoutingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminShuttleService {

    private final ShuttleRouteRepository shuttleRouteRepository;
    private final ShuttleStopRepository shuttleStopRepository;
    private final ShuttleRoutePointRepository shuttleRoutePointRepository;
    private final RoutingService routingService;

    public AdminShuttleService(ShuttleRouteRepository shuttleRouteRepository,
            ShuttleStopRepository shuttleStopRepository,
            ShuttleRoutePointRepository shuttleRoutePointRepository,
            RoutingService routingService) {
        this.shuttleRouteRepository = shuttleRouteRepository;
        this.shuttleStopRepository = shuttleStopRepository;
        this.shuttleRoutePointRepository = shuttleRoutePointRepository;
        this.routingService = routingService;
    }

    @Transactional
    public ShuttleRouteDetailResponse createRoute(ShuttleRouteRequest request) {
        ShuttleRoute route = new ShuttleRoute();
        route.setName(request.name());
        route.setPlateNumber(request.plateNumber());
        route.setDriverName(request.driverName());
        route.setDriverPhone(request.driverPhone());
        shuttleRouteRepository.save(route);

        List<ShuttleStop> stops = shuttleStopRepository.saveAll(toStops(request.stops(), route));
        saveGeometryPoints(route, request.geometryPoints());
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
        if (request.driverName() != null) {
            route.setDriverName(request.driverName());
        }
        if (request.driverPhone() != null) {
            route.setDriverPhone(request.driverPhone());
        }
        shuttleRouteRepository.save(route);

        shuttleStopRepository.deleteByRouteId(routeId);
        List<ShuttleStop> stops = shuttleStopRepository.saveAll(toStops(request.stops(), route));

        shuttleRoutePointRepository.deleteByRouteId(routeId);
        saveGeometryPoints(route, request.geometryPoints());

        return toDetailResponse(route, stops);
    }

    public RouteMatchResponse matchRouteGeometry(RouteMatchRequest request) {
        List<Coordinate> points = request.points().stream()
                .map(p -> new Coordinate(p.lat(), p.lng()))
                .toList();
        List<Coordinate> matched = routingService.matchGeometry(points)
                .orElseThrow(() -> new RouteMatchFailedException(
                        "Rota eşleştirilemedi, noktaları kontrol edip tekrar deneyin"));
        return new RouteMatchResponse(matched);
    }

    public List<Coordinate> getGeometryPoints(Integer routeId) {
        if (!shuttleRouteRepository.existsById(routeId)) {
            throw new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId);
        }
        return shuttleRoutePointRepository.findByRouteIdOrderByOrderIndexAsc(routeId).stream()
                .map(p -> new Coordinate(p.getLatitude(), p.getLongitude()))
                .toList();
    }

    private void saveGeometryPoints(ShuttleRoute route, List<RoutePointRequest> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        List<ShuttleRoutePoint> entities = points.stream().map(p -> {
            ShuttleRoutePoint point = new ShuttleRoutePoint();
            point.setRoute(route);
            point.setLatitude(p.lat());
            point.setLongitude(p.lng());
            return point;
        }).toList();
        for (int i = 0; i < entities.size(); i++) {
            entities.get(i).setOrderIndex(i + 1);
        }
        shuttleRoutePointRepository.saveAll(entities);
    }

    @Transactional
    public ShuttleRoutePlateResponse updatePlate(Integer routeId, PlateUpdateRequest request) {
        ShuttleRoute route = shuttleRouteRepository.findById(routeId)
                .orElseThrow(() -> new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId));
        route.setPlateNumber(request.plateNumber());
        shuttleRouteRepository.save(route);
        return new ShuttleRoutePlateResponse(route.getId(), route.getName(), route.getPlateNumber());
    }

    @Transactional
    public void deleteRoute(Integer routeId) {
        if (!shuttleRouteRepository.existsById(routeId)) {
            throw new ShuttleRouteNotFoundException("Servis guzergahi bulunamadi, id: " + routeId);
        }
        shuttleStopRepository.deleteByRouteId(routeId);
        shuttleRouteRepository.deleteById(routeId);
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
        return new ShuttleRouteDetailResponse(route.getId(), route.getName(), route.getPlateNumber(),
                route.getDriverName(), route.getDriverPhone(), stopResponses);
    }
}
