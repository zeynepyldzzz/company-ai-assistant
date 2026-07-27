package com.company.assistant.directory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.assistant.common.ErrorResponse;

import jakarta.validation.Valid;

/**
 * #84 (Hafta 4): POST/PUT/DELETE /admin/departments (FR-68-71).
 */
@RestController
@RequestMapping("/admin/departments")
@PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    public AdminDepartmentController(AdminDepartmentService adminDepartmentService) {
        this.adminDepartmentService = adminDepartmentService;
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody AdminDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDepartmentService.create(request));
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Integer id, @Valid @RequestBody AdminDepartmentRequest request) {
        return adminDepartmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        adminDepartmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DepartmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("DEPARTMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleManagerNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("EMPLOYEE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DepartmentInUseException.class)
    public ResponseEntity<ErrorResponse> handleInUse(DepartmentInUseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DEPARTMENT_IN_USE", ex.getMessage()));
    }
}
