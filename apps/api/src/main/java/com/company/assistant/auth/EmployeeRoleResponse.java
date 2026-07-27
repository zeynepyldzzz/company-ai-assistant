package com.company.assistant.auth;

// C-11 (#85): PUT /admin/users/{id}/roles yaniti.
public record EmployeeRoleResponse(Integer employeeId, String employeeName, Integer roleId, String roleName) {
}
