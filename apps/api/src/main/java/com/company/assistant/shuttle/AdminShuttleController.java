package com.company.assistant.shuttle;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.routing.Coordinate;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Integer id) {
        adminShuttleService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    // B-31: admin haritada cizdigi ham noktalari gercek yola oturtmak icin
    // onizleme amacli - henuz kaydedilmemis (id'si olmayan) yeni bir
    // guzergah icin de calisir.
    @PostMapping("/match-geometry")
    public RouteMatchResponse matchGeometry(@Valid @RequestBody RouteMatchRequest request) {
        return adminShuttleService.matchRouteGeometry(request);
    }

    // B-31: public /shuttle-routes/{id}/geometry'nin aksine (o her zaman bir
    // cizgi doner, kayit yoksa OSRM/duz-cizgi fallback'i ile) bu uc SADECE
    // gercekten kaydedilmis manuel noktalari doner (yoksa bos liste) -
    // duzenleme formu, hic cizilmemis bir guzergahi "eslesmis" gibi
    // yanlislikla gostermesin diye.
    @GetMapping("/{id}/geometry-points")
    public List<Coordinate> getGeometryPoints(@PathVariable Integer id) {
        return adminShuttleService.getGeometryPoints(id);
    }

    @ExceptionHandler(ShuttleRouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ShuttleRouteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("SHUTTLE_ROUTE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RouteMatchFailedException.class)
    public ResponseEntity<ErrorResponse> handleMatchFailed(RouteMatchFailedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("ROUTE_MATCH_FAILED", ex.getMessage()));
    }
}
