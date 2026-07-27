package com.company.assistant.vehicle;

public class VehicleResponse {

    private Integer id;
    private String plate;
    private String model;
    private MaintenanceStatus maintenanceStatus;

    public VehicleResponse(Vehicle vehicle) {
        this.id = vehicle.getId();
        this.plate = vehicle.getPlate();
        this.model = vehicle.getModel();
        this.maintenanceStatus = vehicle.getMaintenanceStatus();
    }

    public Integer getId() { return id; }
    public String getPlate() { return plate; }
    public String getModel() { return model; }
    public MaintenanceStatus getMaintenanceStatus() { return maintenanceStatus; }
}
