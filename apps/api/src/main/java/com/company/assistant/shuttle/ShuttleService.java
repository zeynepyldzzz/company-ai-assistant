package com.company.assistant.shuttle;

import com.company.assistant.common.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShuttleService {

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
}
