package com.company.assistant.shuttle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShuttleRoutePointRepository extends JpaRepository<ShuttleRoutePoint, Integer> {

    List<ShuttleRoutePoint> findByRouteIdOrderByOrderIndexAsc(Integer routeId);

    void deleteByRouteId(Integer routeId);
}
