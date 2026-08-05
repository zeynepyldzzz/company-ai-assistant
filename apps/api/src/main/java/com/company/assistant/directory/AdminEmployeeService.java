package com.company.assistant.directory;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.assistant.auth.Role;
import com.company.assistant.auth.RoleRepository;
import com.company.assistant.auth.TemporaryPasswordGenerator;
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
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    public AdminEmployeeService(EmployeeRepository employeeRepository,
                                 DepartmentRepository departmentRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 TotpService totpService,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
    }

    /**
     * A-29 (#178): sifre artik OPSIYONEL. Gonderilmezse sistem gecici bir sifre uretir ve
     * kullaniciyi ilk giriste degistirmeye zorlar.
     *
     * <p>Gerekce: admin'in calisanin KALICI sifresini bilmesi, sifreyi bir kimlik dogrulama
     * araci olmaktan cikarir — admin o hesapla islem yapabilir ve loglarda calisanin kendisi
     * gorunur. Gecici sifre bu baglantiyi ilk giriste koparir.
     *
     * <p>Admin yine de sifre gonderebilir; o durumda eski davranis aynen korunur
     * (mustChangePassword false kalir). Geriye donuk uyumluluk icin bilincli.
     */
    @Transactional
    public AdminEmployeeCreateResponse create(AdminEmployeeRequest request) {
        Employee employee = new Employee();
        applyRequest(employee, request);
        employee.setActive(true);

        String generatedPassword = assignTemporaryPassword(employee);

        EmployeeResponse saved = new EmployeeResponse(employeeRepository.save(employee));
        return new AdminEmployeeCreateResponse(saved, generatedPassword);
    }

    /**
     * A-29 (#178): sifre sifirlama. Calisan sifresini unuttugunda admin yeni bir GECICI sifre
     * uretir; kullanici ilk girisinde kendi sifresini belirler.
     *
     * <p>Admin'in kalici sifreyi bilmesi burada da onlenmis oluyor — olusturma akisiyla ayni
     * kural, yalnizca farkli kapi.
     */
    @Transactional
    public AdminEmployeeCreateResponse resetPassword(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Çalışan bulunamadı: " + id));

        String generatedPassword = assignTemporaryPassword(employee);

        EmployeeResponse saved = new EmployeeResponse(employeeRepository.save(employee));
        return new AdminEmployeeCreateResponse(saved, generatedPassword);
    }

    /** Uretilen sifre YALNIZCA doner; kayitta hash'i tutulur, duz metin hicbir yerde kalmaz. */
    private String assignTemporaryPassword(Employee employee) {
        String password = temporaryPasswordGenerator.generate();
        employee.setPasswordHash(passwordEncoder.encode(password));
        employee.setMustChangePassword(true);
        return password;
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
        // A-32 (#188): office_status ARTIK YAZILMIYOR. Ofis durumu bugunun calisma
        // duzeninden turetiliyor; buraya yazilan deger hicbir yerde okunmuyordu ama
        // kolonda kalip bir sonraki gelistiriciyi yaniltirdi. Alan istekten de kaldirildi.

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

        // A-29 (#178): sifre burada YAZILMAZ. Olusturmada create() gecici sifre uretir,
        // sifirlamada resetPassword(). Guncelleme akisi sifreye hic dokunmuyor.

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
