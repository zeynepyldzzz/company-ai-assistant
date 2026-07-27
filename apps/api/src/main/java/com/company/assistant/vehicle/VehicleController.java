package com.company.assistant.vehicle;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * B-8: GET /vehicles?available=true - bakimda olmayan araclarin listesi (FR-38, 41)
 */
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> listVehicles(@RequestParam(required = false) Boolean available) {
        return vehicleService.listVehicles(available);
    }
}
