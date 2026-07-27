package com.company.assistant.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * POST/PUT /admin/employees govdesi.
 * roleId ve departmentId opsiyonel (department atanmamis / rol default employee olabilir).
 */
public record AdminEmployeeRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        String phone,
        String officeStatus,
        Integer departmentId,
        Integer roleId) {
}
