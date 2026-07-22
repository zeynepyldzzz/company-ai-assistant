package com.company.assistant.shuttle;

import com.company.assistant.common.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * FR-73: Servis guzergahi yonetimi. Yalnizca shuttle_admin (veya system_admin,
 * V3 seed'inde tum modul izinlerine sahip) admin uclarina erisebilir.
 */
@RestController
@RequestMapping("/admin/shuttle-routes")
@PreAuthorize("hasAuthority('ROLE_SHUTTLE_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminShuttleController {

    private final AdminShuttleService adminShuttleService;

    public AdminShuttleController(AdminShuttleService adminShuttleService) {
        this.adminShuttleService = adminShuttleService;
    }

    @PostMapping
    public ResponseEntity<ShuttleRouteDetailResponse> createRoute(@Valid @RequestBody ShuttleRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminShuttleService.createRoute(request));
    }

    @PutMapping("/{id}")
    public ShuttleRouteDetailResponse updateRoute(
            @PathVariable Integer id, @Valid @RequestBody ShuttleRouteRequest request) {
        return adminShuttleService.updateRoute(id, request);
    }

    @PutMapping("/{id}/plate")
    public ShuttleRoutePlateResponse updatePlate(
            @PathVariable Integer id, @Valid @RequestBody PlateUpdateRequest request) {
        return adminShuttleService.updatePlate(id, request);
    }

    @ExceptionHandler(ShuttleRouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ShuttleRouteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("SHUTTLE_ROUTE_NOT_FOUND", ex.getMessage()));
    }
}
