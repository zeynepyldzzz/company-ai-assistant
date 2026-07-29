package com.company.assistant.shuttle;

public class ShuttleRouteResponse {

    private Integer id;
    private String name;
    private String plateNumber;
    private String driverName;
    private String driverPhone;

    public ShuttleRouteResponse(ShuttleRoute route) {
        this.id = route.getId();
        this.name = route.getName();
        this.plateNumber = route.getPlateNumber();
        this.driverName = route.getDriverName();
        this.driverPhone = route.getDriverPhone();
    }

    public Integer getId() { return id; }
    public String getName() { return name; }
    public String getPlateNumber() { return plateNumber; }
    public String getDriverName() { return driverName; }
    public String getDriverPhone() { return driverPhone; }
}
