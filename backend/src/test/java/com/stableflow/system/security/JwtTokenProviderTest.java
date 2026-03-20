package com.stableflow.system.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stableflow.system.config.SecurityProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateTokenWithConfiguredTtl() throws Exception {
        SecurityProperties securityProperties = new SecurityProperties(
            "change-me-in-env",
            List.of("http://localhost:5173"),
            Duration.ofMinutes(30)
        );
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(securityProperties);

        String token = jwtTokenProvider.generateToken(new CurrentMerchant(100L, "merchant@example.com"));
        Date expiration = Jwts.parser()
            .verifyWith(buildSigningKey(securityProperties.jwtSecret()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();
        Date issuedAt = Jwts.parser()
            .verifyWith(buildSigningKey(securityProperties.jwtSecret()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getIssuedAt();

        long ttlMillis = expiration.getTime() - issuedAt.getTime();
        assertEquals(Duration.ofMinutes(30).toMillis(), ttlMillis);
        assertTrue(expiration.after(issuedAt));
    }

    @Test
    void shouldFallbackToDefaultTtlWhenConfigMissing() throws Exception {
        SecurityProperties securityProperties = new SecurityProperties(
            "change-me-in-env",
            List.of("http://localhost:5173"),
            null
        );
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(securityProperties);

        String token = jwtTokenProvider.generateToken(new CurrentMerchant(100L, "merchant@example.com"));
        Date expiration = Jwts.parser()
            .verifyWith(buildSigningKey(securityProperties.jwtSecret()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getExpiration();
        Date issuedAt = Jwts.parser()
            .verifyWith(buildSigningKey(securityProperties.jwtSecret()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getIssuedAt();

        long ttlMillis = expiration.getTime() - issuedAt.getTime();
        assertEquals(Duration.ofHours(12).toMillis(), ttlMillis);
    }

    private SecretKey buildSigningKey(String secret) throws NoSuchAlgorithmException {
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(hashed);
    }
}
