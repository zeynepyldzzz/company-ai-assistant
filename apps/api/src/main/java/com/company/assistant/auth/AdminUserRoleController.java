package com.company.assistant.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.company.assistant.common.ErrorResponse;
import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;
import com.company.assistant.directory.RoleNotFoundException;

import jakarta.validation.Valid;

/**
 * C-11 (#85): PUT /admin/users/{id}/roles - sadece system_admin kullaniciya
 * rol atayabilir/degistirebilir (FR-80-82). Genel /admin/employees CRUD'undan
 * (hr_admin de erisebilir) bilerek ayri tutuldu: rol degisikligi daha
 * hassas bir yetki islemi.
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('ROLE_SYSTEM_ADMIN')")
public class AdminUserRoleController {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;

    private final TotpService totpService;

    public AdminUserRoleController(EmployeeRepository employeeRepository, RoleRepository roleRepository,
                                    TotpService totpService) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.totpService = totpService;
    }

    @PutMapping("/{id}/roles")
    public EmployeeRoleResponse updateRole(@PathVariable Integer id, @Valid @RequestBody UpdateRoleRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Kullanici bulunamadi: " + id));
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadi: " + request.roleId()));

        employee.setRole(role);

        // C-12 (#120): bir calisan buradan admin turu bir role terfi ettirilirse
        // (once employee/rolsuzken), giriste 2FA zorunlu olacagi icin TOTP
        // secret'i simdi uretiyoruz (AdminEmployeeService.applyRequest ile ayni
        // mantik). Zaten bir secret'i varsa dokunmuyoruz.
        boolean isAdminRole = !"employee".equalsIgnoreCase(role.getName());
        if (isAdminRole && employee.getTotpSecret() == null) {
            employee.setTotpSecret(totpService.generateSecret());
            employee.setTotpEnabled(false);
        }

        employeeRepository.save(employee);

        return new EmployeeRoleResponse(employee.getId(), employee.getName(), role.getId(), role.getName());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("ROLE_NOT_FOUND", ex.getMessage()));
    }
}
