package com.company.assistant.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public String generateAccessToken(long employeeId, String role, String subRole) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(employeeId))
                .claim("role", role)
                .claim("subRole", subRole)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Geçersiz/süresi dolmuş token'da JwtException fırlatır.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 2FA ara adimi icin kisa omurlu (5 dk) challenge token uretir.
     */
    public String generateChallengeToken(Integer employeeId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(employeeId))
                .claim("purpose", "2fa")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(5, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Challenge token'i dogrular; gecerliyse employee id'yi doner.
     */
    public Integer parseChallengeToken(String token) {
        Claims claims = parseAndValidate(token);
        if (!"2fa".equals(claims.get("purpose", String.class))) {
            throw new JwtException("Not a challenge token");
        }
        return Integer.valueOf(claims.getSubject());
    }
}
