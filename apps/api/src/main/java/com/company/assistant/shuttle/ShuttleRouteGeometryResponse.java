package com.company.assistant.shuttle;

import com.company.assistant.routing.Coordinate;

import java.util.List;

public class ShuttleRouteGeometryResponse {

    private Integer routeId;
    private List<Coordinate> coordinates;

    public ShuttleRouteGeometryResponse(Integer routeId, List<Coordinate> coordinates) {
        this.routeId = routeId;
        this.coordinates = coordinates;
    }

    public Integer getRouteId() { return routeId; }
    public List<Coordinate> getCoordinates() { return coordinates; }
}
