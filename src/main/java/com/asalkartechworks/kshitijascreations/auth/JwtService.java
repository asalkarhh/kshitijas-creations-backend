package com.asalkartechworks.kshitijascreations.auth;

import com.asalkartechworks.kshitijascreations.config.AppProperties;
import com.asalkartechworks.kshitijascreations.domain.entity.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final AppProperties.Jwt properties;

    public JwtService(AppProperties appProperties) {
        this.properties = appProperties.getJwt();
        if (!StringUtils.hasText(properties.getSecret())) {
            throw new IllegalStateException("JWT_SECRET must be configured");
        }
        byte[] secretBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public JwtToken generateToken(AdminUser admin) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getExpiration());
        String token = Jwts.builder()
                .subject(admin.getEmail())
                .claim("role", admin.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new JwtToken(token, expiresAt);
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equalsIgnoreCase(userDetails.getUsername())
                && claims.getExpiration().toInstant().isAfter(Instant.now())
                && userDetails.isEnabled();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record JwtToken(String value, Instant expiresAt) {
    }
}
