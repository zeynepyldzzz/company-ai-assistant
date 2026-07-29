package com.company.assistant.auth;

import java.util.Optional;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.company.assistant.config.SecurityConfig;
import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// C-12 (#120): yeni olusturulan admin turu bir kullanicinin TOTP'ye kayitli
// olmadan hicbir zaman giris yapamadigi sorunu icin eklenen self-service
// enrollment akisi (/auth/2fa/qr, /2fa/verify'in enrollment'i tamamlamasi).
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private TotpService totpService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void jwtFiltresiniGecirgenYap() throws Exception {
        doAnswer(inv -> {
            ((FilterChain) inv.getArgument(2)).doFilter(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    private static Employee yeniAdmin(boolean totpEnabled, String totpSecret) {
        Employee employee = new Employee();
        employee.setId(7);
        employee.setName("Yeni Admin");
        employee.setEmail("yeniadmin@company.com");
        employee.setActive(true);
        employee.setPasswordHash("$2a$10$hash");
        Role role = new Role();
        role.setId(3);
        role.setName("fleet_admin");
        employee.setRole(role);
        employee.setTotpSecret(totpSecret);
        employee.setTotpEnabled(totpEnabled);
        return employee;
    }

    @Test
    void girisSirasinda_enrollmentTamamlanmamisAdminIcinEnrollmentRequiredTrueDoner() throws Exception {
        Employee employee = yeniAdmin(false, "SECRET123");
        when(employeeRepository.findByEmail("yeniadmin@company.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateChallengeToken(7)).thenReturn("challenge-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"yeniadmin@company.com\", \"password\": \"gizli\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoFactorRequired").value(true))
                .andExpect(jsonPath("$.enrollmentRequired").value(true));
    }

    @Test
    void girisSirasinda_kayitliAdminIcinEnrollmentRequiredFalseDoner() throws Exception {
        Employee employee = yeniAdmin(true, "SECRET123");
        when(employeeRepository.findByEmail("yeniadmin@company.com")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateChallengeToken(7)).thenReturn("challenge-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"yeniadmin@company.com\", \"password\": \"gizli\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentRequired").value(false));
    }

    @Test
    void qrUcu_gecersizTokenIcin401Doner() throws Exception {
        when(jwtService.parseChallengeToken(anyString())).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        mockMvc.perform(get("/auth/2fa/qr").param("challengeToken", "bozuk"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void qrUcu_zatenKayitliKullaniciIcin409Doner() throws Exception {
        Employee employee = yeniAdmin(true, "SECRET123");
        when(jwtService.parseChallengeToken("challenge-token")).thenReturn(7);
        when(employeeRepository.findById(7)).thenReturn(Optional.of(employee));

        mockMvc.perform(get("/auth/2fa/qr").param("challengeToken", "challenge-token"))
                .andExpect(status().isConflict());
    }

    @Test
    void qrUcu_kayitOlmamisKullaniciIcinPngDoner() throws Exception {
        Employee employee = yeniAdmin(false, "SECRET123");
        when(jwtService.parseChallengeToken("challenge-token")).thenReturn(7);
        when(employeeRepository.findById(7)).thenReturn(Optional.of(employee));
        when(totpService.generateQrCodePng("yeniadmin@company.com", "SECRET123"))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/auth/2fa/qr").param("challengeToken", "challenge-token"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void ilkBasarailiDogrulama_enrollmentiTamamlarVeTotpEnabledTrueOlur() throws Exception {
        Employee employee = yeniAdmin(false, "SECRET123");
        when(jwtService.parseChallengeToken("challenge-token")).thenReturn(7);
        when(employeeRepository.findById(7)).thenReturn(Optional.of(employee));
        when(totpService.verify("SECRET123", "123456")).thenReturn(true);
        when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(7, "admin", "fleet_admin")).thenReturn("access-token");
        when(refreshTokenService.issue(7)).thenReturn("refresh-token");

        mockMvc.perform(post("/auth/2fa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"challengeToken\": \"challenge-token\", \"code\": \"123456\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(employeeRepository).save(argThat(e -> e.isTotpEnabled()));
    }

    private static Employee argThat(java.util.function.Predicate<Employee> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
