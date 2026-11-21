package co.edu.udistrital.acciones.m6portafolio.support;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TokenTestFactory {

    public static final String SECRETO =
            "m6-test-jwt-secret-0123456789abcdef0123456789abcdef0123456789abcdef";
    public static final long USUARIO = 4101L;
    public static final long ADMINISTRADOR = 4102L;
    public static final long SISTEMA = 900000006L;

    private TokenTestFactory() {
    }

    public static JwtProperties propiedades() {
        return new JwtProperties(SECRETO);
    }

    public static String usuario() {
        return token(USUARIO, "INVERSIONISTA");
    }

    public static String administrador() {
        return token(ADMINISTRADOR, "ADMINISTRADOR");
    }

    public static String sistema() {
        return token(SISTEMA, "SISTEMA");
    }

    public static String token(long userId, String... roles) {
        return construir(String.valueOf(userId), Instant.now().plusSeconds(600), List.of(roles),
                clave(SECRETO), Jwts.SIG.HS256);
    }

    public static String tokenHmac512(long userId, String... roles) {
        return construir(String.valueOf(userId), Instant.now().plusSeconds(600), List.of(roles),
                clave(SECRETO), Jwts.SIG.HS512);
    }

    public static String sinExpiracion(long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .signWith(clave(SECRETO), Jwts.SIG.HS256)
                .compact();
    }

    public static String sinRoles(long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(clave(SECRETO), Jwts.SIG.HS256)
                .compact();
    }

    public static String expirado(long userId) {
        return construir(String.valueOf(userId), Instant.now().minusSeconds(120), List.of("INVERSIONISTA"),
                clave(SECRETO), Jwts.SIG.HS256);
    }

    public static String conSubject(String subject) {
        return construir(subject, Instant.now().plusSeconds(600), List.of("INVERSIONISTA"),
                clave(SECRETO), Jwts.SIG.HS256);
    }

    public static String conRoles(Object roles) {
        return Jwts.builder()
                .subject(String.valueOf(USUARIO))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .claim("roles", roles)
                .signWith(clave(SECRETO), Jwts.SIG.HS256)
                .compact();
    }

    public static String firmaAjena(long userId) {
        return construir(String.valueOf(userId), Instant.now().plusSeconds(600), List.of("INVERSIONISTA"),
                clave(SECRETO + "-ajena"), Jwts.SIG.HS256);
    }

    public static String malformado() {
        return "esto-no-es-un-jwt";
    }

    private static String construir(String subject, Instant expiracion, List<String> roles,
                                    SecretKey clave, SecureDigestAlgorithm<SecretKey, ?> algoritmo) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiracion))
                .claim("roles", roles)
                .signWith(clave, algoritmo)
                .compact();
    }

    private static SecretKey clave(String secreto) {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}
