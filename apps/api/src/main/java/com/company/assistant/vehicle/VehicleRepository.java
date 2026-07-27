package com.company.assistant.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    List<Vehicle> findByMaintenanceStatus(MaintenanceStatus maintenanceStatus);

    boolean existsByPlate(String plate);
}
