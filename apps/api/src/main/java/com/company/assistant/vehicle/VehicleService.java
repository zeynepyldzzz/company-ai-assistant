package com.company.assistant.vehicle;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    // FR-38/41: available=true -> yalnizca bakimda olmayan araclar listelenir.
    public List<VehicleResponse> listVehicles(Boolean available) {
        List<Vehicle> vehicles = Boolean.TRUE.equals(available)
                ? vehicleRepository.findByMaintenanceStatus(MaintenanceStatus.AVAILABLE)
                : vehicleRepository.findAll();
        return vehicles.stream().map(VehicleResponse::new).toList();
    }
}
