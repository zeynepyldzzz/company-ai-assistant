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
 * #84 (Hafta 4): POST/PUT/DELETE /admin/employees (FR-68-71).
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ister; method guard bunu
 * daraltir: sadece hr_admin / system_admin (issue kabul kriteri).
 */
@RestController
@RequestMapping("/admin/employees")
@PreAuthorize("hasAuthority('ROLE_HR_ADMIN') or hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    public AdminEmployeeController(AdminEmployeeService adminEmployeeService) {
        this.adminEmployeeService = adminEmployeeService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody AdminEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminEmployeeService.create(request));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Integer id, @Valid @RequestBody AdminEmployeeRequest request) {
        return adminEmployeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        adminEmployeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("EMPLOYEE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDepartmentNotFound(DepartmentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("DEPARTMENT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("ROLE_NOT_FOUND", ex.getMessage()));
    }
}
