package com.company.assistant.directory;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.common.PagedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public PagedResponse<DepartmentResponse> searchDepartments(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return departmentService.searchDepartments(search, page, pageSize);
    }

    @GetMapping("/{id}")
    public DepartmentResponse getDepartmentById(@PathVariable Integer id) {
        return departmentService.getDepartmentById(id);
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DepartmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("DEPARTMENT_NOT_FOUND", ex.getMessage()));
    }
}
