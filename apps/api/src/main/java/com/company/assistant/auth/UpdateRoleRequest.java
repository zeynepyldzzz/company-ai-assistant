package com.company.assistant.auth;

import jakarta.validation.constraints.NotNull;

// C-11 (#85): PUT /admin/users/{id}/roles govdesi.
public record UpdateRoleRequest(@NotNull Integer roleId) {
}
