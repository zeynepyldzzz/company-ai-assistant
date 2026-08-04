package com.company.assistant.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.company.assistant.directory.Employee;
import com.company.assistant.directory.EmployeeRepository;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final EmployeeRepository employeeRepository;
    /**
     * A-29 (#178): sifre uzunluk alt siniri. TemporaryPasswordGenerator 12 karakter uretir,
     * yani sistem sifreleri bu siniri rahatca asar; kural kullanicinin KENDI belirledigi
     * sifre icin gecerli.
     */
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;

    public AuthController(EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            TotpService totpService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthDtos.LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        if (!employee.isActive()
                || employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw invalidCredentials();
        }

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);

        // Admin ise: token verme, 2FA adimina yonlendir
        if ("admin".equals(roleInfo.role())) {
            String challengeToken = jwtService.generateChallengeToken(employee.getId());
            // C-12 (#120): totpEnabled=false -> bu kullanici henuz authenticator
            // app'ine kayit olmadi (ornegin yeni olusturulmus bir admin hesabi).
            // Frontend'e QR ekranini once gostermesi gerektigini soyluyoruz.
            return ResponseEntity.ok(new AuthDtos.TwoFactorRequiredResponse(
                    true, challengeToken, !employee.isTotpEnabled()));
        }

        String accessToken = jwtService.generateAccessToken(
                employee.getId(), roleInfo.role(), roleInfo.subRole());
        String refreshToken = refreshTokenService.issue(employee.getId());

        return ResponseEntity.ok(new AuthDtos.LoginResponse(accessToken, refreshToken,
                new AuthDtos.UserDto(employee.getId(), employee.getName(),
                        employee.getEmail(), roleInfo.role(), roleInfo.subRole()),
                employee.isMustChangePassword()));
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

    /**
     * C-12 (#120): self-service TOTP enrollment - kullanici henuz authenticator
     * app'ine kayitli degilse (totpEnabled=false), giris akisinin 2. adiminda
     * frontend bu ucu cagirip QR kodu ekranda gosterir. challengeToken zaten
     * kisa omurlu (5 dk) ve tek kullanicinin kimligini tasidigi icin ayri bir
     * auth header'a gerek yok - <img> etiketinden dogrudan cagirilabilir.
     */
    @GetMapping(value = "/2fa/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> twoFactorQrCode(@RequestParam String challengeToken) {
        Integer employeeId;
        try {
            employeeId = jwtService.parseChallengeToken(challengeToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid challenge");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid challenge"));

        if (employee.getTotpSecret() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Bu kullanıcı için TOTP secret üretilmemiş");
        }
        if (employee.isTotpEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "2FA zaten kurulu, tekrar QR gösterilmez");
        }

        byte[] png = totpService.generateQrCodePng(employee.getEmail(), employee.getTotpSecret());
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @PostMapping("/2fa/verify")
    public AuthDtos.LoginResponse verifyTwoFactor(@RequestBody AuthDtos.TwoFactorVerifyRequest request) {
        Integer employeeId;
        try {
            employeeId = jwtService.parseChallengeToken(request.challengeToken());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid challenge");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid challenge"));

        if (employee.getTotpSecret() == null
                || !totpService.verify(employee.getTotpSecret(), request.code())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
        }

        // C-12 (#120): ilk basarili dogrulama = enrollment'in tamamlanmasi.
        if (!employee.isTotpEnabled()) {
            employee.setTotpEnabled(true);
            employee = employeeRepository.save(employee);
        }

        AuthDtos.RoleInfo roleInfo = AuthDtos.RoleInfo.from(employee);
        String accessToken = jwtService.generateAccessToken(
                employee.getId(), roleInfo.role(), roleInfo.subRole());
        String refreshToken = refreshTokenService.issue(employee.getId());

        // A-29 (#178): admin akisinda sifre degisimi 2FA'nin YERINE gecmez; once kimlik
        // dogrulanir, sonra gecici sifre degistirilir.
        return new AuthDtos.LoginResponse(accessToken, refreshToken,
                new AuthDtos.UserDto(employee.getId(), employee.getName(),
                        employee.getEmail(), roleInfo.role(), roleInfo.subRole()),
                employee.isMustChangePassword());
    }

    /**
     * A-29 (#178): sifre degistirme. Bu uc daha once HIC yoktu — calisan, admin'in
     * belirledigi sifreyi degistiremiyordu.
     *
     * <p>Mevcut sifre dogrulanir: token'a sahip olmak yeterli degil, cunku acik kalmis bir
     * oturumdan sifre degistirilebilmesi hesabin tamamen ele gecirilmesi demektir.
     *
     * <p>SecurityConfig'e satir eklenmedi; permitAll listesi tek tek uclari sayiyor ve bu uc
     * anyRequest().authenticated() varsayilanina dusuyor.
     */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestBody AuthDtos.ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {

        Employee employee = employeeRepository.findById(Integer.valueOf(authentication.getName()))
                .filter(Employee::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Session invalid"));

        if (employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mevcut şifre hatalı");
        }

        String newPassword = request.newPassword();
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Yeni şifre en az " + MIN_PASSWORD_LENGTH + " karakter olmalıdır");
        }
        // Gecici sifreyi "degistirdim" deyip aynisini yazmak akisi anlamsiz kilardi.
        if (passwordEncoder.matches(newPassword, employee.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Yeni şifre mevcut şifreyle aynı olamaz");
        }

        employee.setPasswordHash(passwordEncoder.encode(newPassword));
        employee.setMustChangePassword(false);
        employeeRepository.save(employee);

        return ResponseEntity.noContent().build();
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
