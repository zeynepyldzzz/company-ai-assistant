package com.company.assistant.shuttle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShuttleStopRepository extends JpaRepository<ShuttleStop, Integer> {

    List<ShuttleStop> findByRouteIdOrderByOrderIndexAsc(Integer routeId);

    void deleteByRouteId(Integer routeId);

    List<ShuttleStop> findByLatitudeIsNotNullAndLongitudeIsNotNull();
}
