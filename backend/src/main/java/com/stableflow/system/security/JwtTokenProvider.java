package com.stableflow.system.security;

import com.stableflow.system.config.SecurityProperties;
import com.stableflow.system.exception.BusinessException;
import com.stableflow.system.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Duration TOKEN_TTL = Duration.ofHours(12);
    private final SecretKey signingKey;

    public JwtTokenProvider(SecurityProperties securityProperties) {
        this.signingKey = buildSigningKey(securityProperties.jwtSecret());
    }

    public String generateToken(CurrentMerchant currentMerchant) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(currentMerchant.merchantId()))
            .claim("email", currentMerchant.email())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(TOKEN_TTL)))
            .signWith(signingKey)
            .compact();
    }

    public CurrentMerchant parseToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return new CurrentMerchant(Long.valueOf(claims.getSubject()), claims.get("email", String.class));
    }

    private SecretKey buildSigningKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new BusinessException(ErrorCode.CONFIGURATION_ERROR, "JWT secret is required");
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ErrorCode.CONFIGURATION_ERROR, "Failed to initialize JWT signing key");
        }
    }
}
