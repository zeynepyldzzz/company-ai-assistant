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
    // C-12 (#120): enrollmentRequired=true ise frontend once QR kodu gostermeli
    // (GET /auth/2fa/qr?challengeToken=...), kullanici authenticator app'ine
    // kaydettikten sonra kodu girip /2fa/verify ile hem kaydini tamamlar hem
    // giris yapar. false ise dogrudan kod giris ekrani yeterli.
    public record TwoFactorRequiredResponse(
            boolean twoFactorRequired, String challengeToken, boolean enrollmentRequired) {
    }

    public record TwoFactorVerifyRequest(String challengeToken, String code) {
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
