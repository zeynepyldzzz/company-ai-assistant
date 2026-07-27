package com.company.assistant.auth;

// C-11 (#85): GET /admin/roles yaniti.
public record RoleResponse(Integer id, String name) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
