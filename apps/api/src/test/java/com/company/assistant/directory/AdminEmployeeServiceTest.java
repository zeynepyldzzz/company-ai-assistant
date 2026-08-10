package com.company.assistant.directory;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.company.assistant.auth.Role;
import com.company.assistant.auth.RoleRepository;
import com.company.assistant.auth.TemporaryPasswordGenerator;
import com.company.assistant.auth.TotpService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// C-12 (#120): sifre atama + admin turu roller icin otomatik TOTP secret
// uretimi bu servisin (applyRequest) sorumlulugu - burada dogrulaniyor.
@ExtendWith(MockitoExtension.class)
class AdminEmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TotpService totpService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AdminEmployeeService service;

    @BeforeEach
    void setUp() {
        service = new AdminEmployeeService(
                employeeRepository, departmentRepository, roleRepository, passwordEncoder,
                totpService, new TemporaryPasswordGenerator());
        lenient().when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // A-29 (#178): admin ARTIK HICBIR YOLLA sifre belirlemiyor — istek govdesinde password
    // alani bile yok. Olusturmada sistem her zaman gecici sifre uretir.
    //
    // Gerekce: admin'in calisanin sifresini bilmesi, sifreyi kimlik dogrulama araci olmaktan
    // cikarir. Once "admin isterse girsin" seklinde birakilmisti; iki yolun da sonucu gecici
    // sifre oldugu icin birincisi tamamen kaldirildi ve kural tek cumleye indi.
    @Test
    void olusturmada_sistemHerZamanGeciciSifreUretir() {
        AdminEmployeeRequest request = new AdminEmployeeRequest(
                "Test", "Calisan", "test@company.com", null, null, null);

        AdminEmployeeCreateResponse response = service.create(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        org.mockito.Mockito.verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertThat(response.generatedPassword()).isNotBlank();
        assertThat(saved.isMustChangePassword()).isTrue();
        // Uretilen sifre DUZ METIN saklanmaz; yalnizca hash'i kaydedilir.
        assertThat(saved.getPasswordHash()).isNotEqualTo(response.generatedPassword());
        assertThat(passwordEncoder.matches(response.generatedPassword(), saved.getPasswordHash())).isTrue();
    }

    // "Calisan sifresini unuttu" senaryosu: admin yeni bir GECICI sifre uretir, kalici
    // sifreyi yine bilmez.
    @Test
    void sifreSifirlamada_yeniGeciciSifreUretilir() {
        Employee existing = new Employee();
        existing.setId(7);
        existing.setFirstName("Mevcut");
        existing.setLastName("Calisan");
        existing.setEmail("mevcut@company.com");
        existing.setPasswordHash("$2a$10$eskiHash");
        when(employeeRepository.findById(7)).thenReturn(Optional.of(existing));

        AdminEmployeeCreateResponse response = service.resetPassword(7);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        org.mockito.Mockito.verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertThat(response.generatedPassword()).isNotBlank();
        assertThat(saved.isMustChangePassword()).isTrue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("$2a$10$eskiHash");
        assertThat(passwordEncoder.matches(response.generatedPassword(), saved.getPasswordHash())).isTrue();
    }

    @Test
    void rolsuzCalisanOlusturulursa_totpSecretUretilmez() {
        AdminEmployeeRequest request = new AdminEmployeeRequest(
                "Test", "Calisan", "test@company.com", null, null, null);

        service.create(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        org.mockito.Mockito.verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getTotpSecret()).isNull();
        org.mockito.Mockito.verifyNoInteractions(totpService);
    }

    @Test
    void adminTuruRolleOlusturulursa_totpSecretOtomatikUretilirVeEnabledFalseKalir() {
        Role fleetAdmin = new Role();
        fleetAdmin.setId(3);
        fleetAdmin.setName("fleet_admin");
        when(roleRepository.findById(3)).thenReturn(Optional.of(fleetAdmin));
        when(totpService.generateSecret()).thenReturn("ABCDEFGHIJKLMNOP");

        AdminEmployeeRequest request = new AdminEmployeeRequest(
                "Yeni", "Admin", "admin2@company.com", null, null, 3);

        service.create(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        org.mockito.Mockito.verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertThat(saved.getTotpSecret()).isEqualTo("ABCDEFGHIJKLMNOP");
        assertThat(saved.isTotpEnabled()).isFalse();
    }

    // Guncelleme akisi sifreye HIC dokunmaz; sifirlama ayri bir uc uzerinden yapiliyor.
    @Test
    void guncellemede_mevcutSifreKorunur() {
        Employee existing = new Employee();
        existing.setId(5);
        existing.setFirstName("Eski");
        existing.setLastName("Isim");
        existing.setEmail("eski@company.com");
        existing.setPasswordHash("$2a$10$mevcutHash");
        when(employeeRepository.findById(5)).thenReturn(Optional.of(existing));

        AdminEmployeeRequest request = new AdminEmployeeRequest(
                "Yeni", "Isim", "eski@company.com", null, null, null);

        service.update(5, request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        org.mockito.Mockito.verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$mevcutHash");
        assertThat(captor.getValue().isMustChangePassword()).isFalse();
        assertThat(captor.getValue().getName()).isEqualTo("Yeni Isim");
    }
}
