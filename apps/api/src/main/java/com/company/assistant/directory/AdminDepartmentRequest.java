package com.company.assistant.directory;

import jakarta.validation.constraints.NotBlank;

/** POST/PUT /admin/departments govdesi. managerId opsiyonel. */
public record AdminDepartmentRequest(
        @NotBlank String name,
        String responsibilities,
        Integer managerId) {
}
