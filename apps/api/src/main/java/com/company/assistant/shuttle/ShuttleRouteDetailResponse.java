package com.company.assistant.shuttle;

import java.util.List;

public class ShuttleRouteDetailResponse {

    private Integer id;
    private String name;
    private String plateNumber;
    private List<ShuttleStopResponse> stops;

    public ShuttleRouteDetailResponse(Integer id, String name, String plateNumber, List<ShuttleStopResponse> stops) {
        this.id = id;
        this.name = name;
        this.plateNumber = plateNumber;
        this.stops = stops;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plateNumber; }
    public List<ShuttleStopResponse> getStops() { return stops; }
}
