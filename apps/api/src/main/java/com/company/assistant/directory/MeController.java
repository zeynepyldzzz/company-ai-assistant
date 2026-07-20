package com.company.assistant.directory;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.company.assistant.auth.AuthDtos;

@RestController
@RequestMapping("/me")
public class MeController {

    private final EmployeeRepository employeeRepository;

    public MeController(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public AuthDtos.UserDto me(Authentication authentication) {
        Integer employeeId = Integer.valueOf(authentication.getName());

        Employee employee = employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Session invalid"));

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);
        return new AuthDtos.UserDto(employee.getId(), employee.getName(),
                employee.getEmail(), roleInfo.role(), roleInfo.subRole());
    }
}