package com.company.assistant.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@RequestBody AuthDtos.LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        if (!employee.isActive()
                || employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw invalidCredentials();
        }

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);

        String accessToken = jwtService.generateAccessToken(
                employee.getId(), roleInfo.role(), roleInfo.subRole());
        String refreshToken = refreshTokenService.issue(employee.getId());

        return new AuthDtos.LoginResponse(accessToken, refreshToken,
                new AuthDtos.UserDto(employee.getId(), employee.getName(),
                        employee.getEmail(), roleInfo.role(), roleInfo.subRole()));
    }

    @PostMapping("/refresh")
    public AuthDtos.RefreshResponse refresh(@RequestBody AuthDtos.RefreshRequest request) {
        RefreshToken stored = refreshTokenService.validate(request.refreshToken())
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        Employee employee = employeeRepository.findById(stored.getEmployeeId())
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // Rotasyon: eskisi iptal, yenisi üretilir
        refreshTokenService.revoke(stored);
        String newRefreshToken = refreshTokenService.issue(employee.getId());

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);
        String accessToken = jwtService.generateAccessToken(
                employee.getId(), roleInfo.role(), roleInfo.subRole());

        return new AuthDtos.RefreshResponse(accessToken, newRefreshToken);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody AuthDtos.RefreshRequest request) {
        refreshTokenService.validate(request.refreshToken())
                .ifPresent(refreshTokenService::revoke);

    }

    @GetMapping("/session")
    public AuthDtos.UserDto session(org.springframework.security.core.Authentication authentication) {
        Integer employeeId = Integer.valueOf(authentication.getName());

        Employee employee = employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Session invalid"));

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);
        return new AuthDtos.UserDto(employee.getId(), employee.getName(),
                employee.getEmail(), roleInfo.role(), roleInfo.subRole());
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
