package com.company.assistant.hr;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.common.PagedResponse;

/**
 * BP-02 / FR-11-13, 51-58. Rol: employee — /hr/** SecurityConfig'te
 * .anyRequest().authenticated() altina dusuyor, ek matcher gerekmez (adminler dahil
 * her authenticated kullanici erisir; hasRole('EMPLOYEE') adminleri yanlislikla dislardi).
 */
@RestController
@RequestMapping("/hr/procedures")
public class HrProcedureController {

    private final HrProcedureService service;

    public HrProcedureController(HrProcedureService service) {
        this.service = service;
    }

    // topic parametresi varsa tekil nesne doner (dokuman §3).
    @GetMapping(params = "topic")
    public HrProcedureDetail getByTopic(@RequestParam String topic) {
        return service.getByTopic(topic);
    }

    // topic yoksa sayfali liste zarfi doner.
    @GetMapping
    public PagedResponse<HrProcedureSummary> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return service.list(page, pageSize);
    }

    @GetMapping("/{id}")
    public HrProcedureDetail getById(@PathVariable int id) {
        return service.getById(id);
    }

    @ExceptionHandler(HrProcedureNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(HrProcedureNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("HR_PROCEDURE_NOT_FOUND", ex.getMessage()));
    }
}
