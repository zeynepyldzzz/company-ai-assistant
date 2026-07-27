package com.company.assistant.vehicle;

import com.company.assistant.common.ErrorResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B-8: POST /reservations, GET /reservations/me, DELETE /reservations/{id} (FR-39, 40)
 */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request, Authentication authentication) {
        ReservationResponse response = reservationService.createReservation(
                currentEmployeeId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public List<ReservationResponse> getMyReservations(Authentication authentication) {
        return reservationService.listMyReservations(currentEmployeeId(authentication));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Integer id, Authentication authentication) {
        reservationService.cancelReservation(currentEmployeeId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    // FR-39/40: kimlik her zaman JWT'den alinir, istek govdesinden/URL'den degil.
    private Integer currentEmployeeId(Authentication authentication) {
        return Integer.valueOf(authentication.getName());
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("VEHICLE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFound(ReservationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESERVATION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ReservationConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("RESERVATION_TIME_CONFLICT", ex.getMessage()));
    }
}
