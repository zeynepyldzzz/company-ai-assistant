package com.company.assistant.shuttle;

public class ShuttleRecommendationResponse {

    private Integer routeId;
    private String routeName;
    private String plateNumber;
    private String driverName;
    private String driverPhone;
    private Integer stopId;
    private String stopName;
    private double distanceKm;
    private int estimatedMinutes;

    public ShuttleRecommendationResponse(ShuttleStop nearestStop, double distanceKm, int estimatedMinutes) {
        ShuttleRoute route = nearestStop.getRoute();
        this.routeId = route.getId();
        this.routeName = route.getName();
        this.plateNumber = route.getPlateNumber();
        this.driverName = route.getDriverName();
        this.driverPhone = route.getDriverPhone();
        this.stopId = nearestStop.getId();
        this.stopName = nearestStop.getName();
        this.distanceKm = distanceKm;
        this.estimatedMinutes = estimatedMinutes;
    }

    public Integer getRouteId() { return routeId; }
    public String getRouteName() { return routeName; }
    public String getPlateNumber() { return plateNumber; }
    public String getDriverName() { return driverName; }
    public String getDriverPhone() { return driverPhone; }
    public Integer getStopId() { return stopId; }
    public String getStopName() { return stopName; }
    public double getDistanceKm() { return distanceKm; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
}
