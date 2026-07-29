package com.company.assistant.shuttle;

import java.util.List;

public class ShuttleRouteDetailResponse {

    private Integer id;
    private String name;
    private String plateNumber;
    private String driverName;
    private String driverPhone;
    private List<ShuttleStopResponse> stops;

    public ShuttleRouteDetailResponse(Integer id, String name, String plateNumber, String driverName,
            String driverPhone, List<ShuttleStopResponse> stops) {
        this.id = id;
        this.name = name;
        this.plateNumber = plateNumber;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.stops = stops;
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plateNumber; }
    public String getDriverName() { return driverName; }
    public String getDriverPhone() { return driverPhone; }
    public List<ShuttleStopResponse> getStops() { return stops; }
}
