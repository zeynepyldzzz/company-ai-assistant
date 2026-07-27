package com.company.assistant.vehicle;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String plate;

    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_status")
    private MaintenanceStatus maintenanceStatus;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public MaintenanceStatus getMaintenanceStatus() { return maintenanceStatus; }
    public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) { this.maintenanceStatus = maintenanceStatus; }
}
