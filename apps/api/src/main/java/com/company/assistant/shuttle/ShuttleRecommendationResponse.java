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
    private double searchLat;
    private double searchLng;

    // B-27: aranan konumun kendisini haritada gostermek icin geocode edilen
    // (veya dogrudan verilen) koordinat da yanitla birlikte dondurulur.
    public ShuttleRecommendationResponse(ShuttleStop nearestStop, double distanceKm, int estimatedMinutes,
            double searchLat, double searchLng) {
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
        this.searchLat = searchLat;
        this.searchLng = searchLng;
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
    public double getSearchLat() { return searchLat; }
    public double getSearchLng() { return searchLng; }
}
