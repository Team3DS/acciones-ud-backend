package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = props.getAccessTtlSeconds();
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}