package com.company.assistant.auth;

import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Service
public class TotpService {

    // C-12 (#120): authenticator app'te ("Google Authenticator" vb.) hesabin
    // altinda gorunecek uygulama adi.
    private static final String ISSUER = "Company AI Assistant";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    public TotpService() {
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    /** Yeni TOTP secret uretir (kullanici kaydinda / 2FA kurulumunda cagrilir). */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /** Kullanicinin girdigi 6 haneli kodu secret'a karsi dogrular. */
    public boolean verify(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    /**
     * C-12 (#120): self-service enrollment icin PNG formatinda QR kod uretir.
     * Kullanici bunu authenticator app'iyle tarayip hesabini kaydeder.
     */
    public byte[] generateQrCodePng(String employeeEmail, String secret) {
        QrData data = new QrData.Builder()
                .label(employeeEmail)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            return qrGenerator.generate(data);
        } catch (QrGenerationException e) {
            throw new IllegalStateException("QR kod üretilemedi", e);
        }
    }
}