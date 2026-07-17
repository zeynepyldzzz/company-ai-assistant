package com.company.assistant.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.repository = repository;
        this.refreshTtlDays = refreshTtlDays;
    }

    /** Yeni refresh token üretir, hash'ini DB'ye yazar, HAM token'ı döner (sadece client görür). */
    public String issue(Integer employeeId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setEmployeeId(employeeId);
        entity.setTokenHash(sha256(rawToken));
        entity.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        repository.save(entity);

        return rawToken;
    }

    /** Ham token'ı doğrular; geçerliyse kaydı döner, değilse boş. */
    public Optional<RefreshToken> validate(String rawToken) {
        return repository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.getRevokedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }

    /** Token'ı iptal eder (logout / rotasyon). */
    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevokedAt(Instant.now());
        repository.save(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}