package com.company.assistant.shuttle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ShuttleStopRepository extends JpaRepository<ShuttleStop, Integer> {

    List<ShuttleStop> findByRouteIdOrderByOrderIndexAsc(Integer routeId);

    /**
     * A-18 (#127): birden fazla guzergahin duraklari TEK sorguda. Chatbot resolver'i
     * onceden hat basina ayri sorgu atiyordu (N+1).
     * JOIN FETCH sart: gruplama route id'sine gore yapiliyor, lazy proxy acilmasin.
     */
    @Query("""
            SELECT s FROM ShuttleStop s
            JOIN FETCH s.route r
            WHERE r.id IN :routeIds
            ORDER BY r.id, s.orderIndex
            """)
    List<ShuttleStop> findByRouteIds(@Param("routeIds") Collection<Integer> routeIds);

    void deleteByRouteId(Integer routeId);

    List<ShuttleStop> findByLatitudeIsNotNullAndLongitudeIsNotNull();
}
