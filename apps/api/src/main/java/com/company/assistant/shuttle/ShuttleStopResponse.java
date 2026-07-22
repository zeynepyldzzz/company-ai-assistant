package com.company.assistant.shuttle;

import java.time.LocalTime;

public class ShuttleStopResponse {

    private Integer id;
    private String name;
    private LocalTime time;
    private Integer orderIndex;
    private Double latitude;
    private Double longitude;

    public ShuttleStopResponse(ShuttleStop stop) {
        this.id = stop.getId();
        this.name = stop.getName();
        this.time = stop.getTime();
        this.orderIndex = stop.getOrderIndex();
        this.latitude = stop.getLatitude();
        this.longitude = stop.getLongitude();
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public LocalTime getTime() { return time; }
    public Integer getOrderIndex() { return orderIndex; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
