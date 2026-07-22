package com.company.assistant.shuttle;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.common.PagedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B-5: Servis guzergahlari, duraklari ve plaka bilgisi (FR-22, 23, 24, 25)
 */
@RestController
@RequestMapping("/shuttle-routes")
public class ShuttleController {

    private final ShuttleService shuttleService;

    public ShuttleController(ShuttleService shuttleService) {
        this.shuttleService = shuttleService;
    }

    @GetMapping
    public PagedResponse<ShuttleRouteResponse> listRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return shuttleService.listRoutes(page, pageSize);
    }

    @GetMapping("/{id}/stops")
    public List<ShuttleStopResponse> getStops(@PathVariable Integer id) {
        return shuttleService.getStops(id);
    }

    @GetMapping("/{id}/plate")
    public ShuttleRoutePlateResponse getPlate(@PathVariable Integer id) {
        return shuttleService.getPlate(id);
    }

    @ExceptionHandler(ShuttleRouteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ShuttleRouteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("SHUTTLE_ROUTE_NOT_FOUND", ex.getMessage()));
    }
}
