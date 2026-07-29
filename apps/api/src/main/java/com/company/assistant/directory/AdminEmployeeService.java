package com.company.assistant.directory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.company.assistant.auth.Role;
import com.company.assistant.auth.RoleRepository;
import com.company.assistant.auth.TotpService;

/**
 * #84 (Hafta 4): POST/PUT/DELETE /admin/employees (FR-68-71).
 * /admin/** yolu SecurityConfig'te hasRole("ADMIN") ile korunuyor; controller
 * seviyesinde hr_admin/system_admin'e daraltilir (PolicyDocumentController deseni).
 *
 * C-12 (#120): sifre atama + admin rolundeki yeni kullanicilar icin otomatik
 * TOTP secret uretimi burada eklendi (bkz. applyRequest).
 */
@Service
public class AdminEmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    public AdminEmployeeService(EmployeeRepository employeeRepository,
                                 DepartmentRepository departmentRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 TotpService totpService) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
    }

    @Transactional
    public EmployeeResponse create(AdminEmployeeRequest request) {
        if (!StringUtils.hasText(request.password())) {
            throw new EmployeePasswordRequiredException(
                    "Yeni çalışan oluşturulurken bir ilk şifre belirlenmelidir.");
        }
        Employee employee = new Employee();
        applyRequest(employee, request);
        employee.setActive(true);
        return new EmployeeResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Integer id, AdminEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Çalışan bulunamadı: " + id));
        applyRequest(employee, request);
        return new EmployeeResponse(employeeRepository.save(employee));
    }

    /**
     * FR-71: calisan silme. Employee birçok tabloda (haftalik duzen, anket yaniti,
     * arac rezervasyonu vb.) referans edildigi icin hard delete FK ihlaline yol
     * acar; is_active=false ile "soft delete" yapiliyor (kolon zaten bu amacla var).
     */
    @Transactional
    public void delete(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Çalışan bulunamadı: " + id));
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private void applyRequest(Employee employee, AdminEmployeeRequest request) {
        employee.setName(request.name());
        employee.setEmail(request.email());
        employee.setPhone(request.phone());
        employee.setOfficeStatus(request.officeStatus());

        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new DepartmentNotFoundException(
                            "Departman bulunamadı: " + request.departmentId()));
            employee.setDepartment(department);
        } else {
            employee.setDepartment(null);
        }

        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                    .orElseThrow(() -> new RoleNotFoundException("Rol bulunamadı: " + request.roleId()));
            employee.setRole(role);
        }

        // C-12 (#120): sifre gonderildiyse (olusturmada zorunlu, guncellemede
        // opsiyonel - bos birakilirsa mevcut sifre korunur) hashleyip yaz.
        if (StringUtils.hasText(request.password())) {
            employee.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        // Admin turu bir role atandiysa (employee disinda herhangi bir rol -
        // bkz. AuthDtos.RoleInfo.from) ve henuz bir TOTP secret'i yoksa, giris
        // sirasinda 2FA zorunlu oldugu icin burada uretip self-service
        // enrollment'a hazir hale getiriyoruz (bkz. AuthController /2fa/qr,
        // /2fa/verify). totpEnabled bilinçli olarak false kalir; kullanici ilk
        // girisinde QR'i tarayip kodu dogruladiginda true'ya cekilir.
        boolean isAdminRole = employee.getRole() != null
                && !"employee".equalsIgnoreCase(employee.getRole().getName());
        if (isAdminRole && employee.getTotpSecret() == null) {
            employee.setTotpSecret(totpService.generateSecret());
            employee.setTotpEnabled(false);
        }
    }
}
