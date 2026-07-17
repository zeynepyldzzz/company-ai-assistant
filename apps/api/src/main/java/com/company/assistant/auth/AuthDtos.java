package com.company.assistant.auth;

import com.company.assistant.directory.Employee;

public class AuthDtos {

    public record LoginRequest(String email, String password) {

    }

    public record UserDto(Integer id, String name, String email,
            String role, String subRole) {

    }

    public record LoginResponse(String accessToken, String refreshToken,
            UserDto user) {

    }

    public record RefreshRequest(String refreshToken) {

    }

    public record RefreshResponse(String accessToken, String refreshToken) {

    }

    public record RoleInfo(String role, String subRole) {

        public static RoleInfo from(Employee employee) {
            String roleName = employee.getRole() != null ? employee.getRole().getName() : "employee";
            return roleName.equals("employee")
                    ? new RoleInfo("employee", null)
                    : new RoleInfo("admin", roleName);
        }
    }

}
