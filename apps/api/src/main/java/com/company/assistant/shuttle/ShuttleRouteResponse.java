package com.company.assistant.shuttle;

public class ShuttleRouteResponse {

    private Integer id;
    private String name;
    private String plateNumber;

    public ShuttleRouteResponse(ShuttleRoute route) {
        this.id = route.getId();
        this.name = route.getName();
        this.plateNumber = route.getPlateNumber();
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plateNumber; }
}
