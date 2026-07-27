package com.company.assistant.vehicle;

import com.company.assistant.common.ErrorResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * FR-74: Arac yonetimi. Yalnizca fleet_admin (veya system_admin, V3 seed'inde tum
 * modul izinlerine sahip) admin uclarina erisebilir.
 */
@RestController
@RequestMapping("/admin/vehicles")
@PreAuthorize("hasAuthority('ROLE_FLEET_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;

    public AdminVehicleController(AdminVehicleService adminVehicleService) {
        this.adminVehicleService = adminVehicleService;
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminVehicleService.createVehicle(request));
    }

    @PutMapping("/{id}")
    public VehicleResponse updateVehicle(@PathVariable Integer id, @Valid @RequestBody VehicleRequest request) {
        return adminVehicleService.updateVehicle(id, request);
    }

    @PutMapping("/{id}/maintenance-status")
    public VehicleResponse updateMaintenanceStatus(
            @PathVariable Integer id, @Valid @RequestBody MaintenanceStatusUpdateRequest request) {
        return adminVehicleService.updateMaintenanceStatus(id, request);
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(VehicleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("VEHICLE_NOT_FOUND", ex.getMessage()));
    }
}
