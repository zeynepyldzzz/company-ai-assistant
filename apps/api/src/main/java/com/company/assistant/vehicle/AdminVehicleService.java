package com.company.assistant.vehicle;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminVehicleService {

    private final VehicleRepository vehicleRepository;

    public AdminVehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    // FR-74: arac ekleme.
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(request.plate());
        vehicle.setModel(request.model());
        vehicle.setMaintenanceStatus(
                request.maintenanceStatus() != null ? request.maintenanceStatus() : MaintenanceStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
        return new VehicleResponse(vehicle);
    }

    // FR-74: arac guncelleme.
    @Transactional
    public VehicleResponse updateVehicle(Integer vehicleId, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Araç bulunamadı, id: " + vehicleId));

        vehicle.setPlate(request.plate());
        vehicle.setModel(request.model());
        if (request.maintenanceStatus() != null) {
            vehicle.setMaintenanceStatus(request.maintenanceStatus());
        }
        vehicleRepository.save(vehicle);
        return new VehicleResponse(vehicle);
    }

    // FR-74/41: bakim durumu isaretleme.
    @Transactional
    public VehicleResponse updateMaintenanceStatus(Integer vehicleId, MaintenanceStatusUpdateRequest request) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Araç bulunamadı, id: " + vehicleId));
        vehicle.setMaintenanceStatus(request.maintenanceStatus());
        vehicleRepository.save(vehicle);
        return new VehicleResponse(vehicle);
    }
}
