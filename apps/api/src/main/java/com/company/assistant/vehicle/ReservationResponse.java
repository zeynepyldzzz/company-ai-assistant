package com.company.assistant.vehicle;

import java.time.LocalDateTime;

public class ReservationResponse {

    private Integer id;
    private Integer vehicleId;
    private String vehiclePlate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReservationStatus status;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.vehicleId = reservation.getVehicle().getId();
        this.vehiclePlate = reservation.getVehicle().getPlate();
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.status = reservation.getStatus();
    }

    public Integer getId() { return id; }
    public Integer getVehicleId() { return vehicleId; }
    public String getVehiclePlate() { return vehiclePlate; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public ReservationStatus getStatus() { return status; }
}
