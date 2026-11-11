package co.edu.udistrital.acciones.m2cuentasfondos.support;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class TokenTestFactory {

    public static final String SECRETO = "test-jwt-secret-0123456789abcdef0123456789abcdef-for-integration";

    private TokenTestFactory() {
    }

    public static JwtProperties propiedades() {
        JwtProperties propiedades = new JwtProperties();
        propiedades.setSecret(SECRETO);
        propiedades.validar();
        return propiedades;
    }

    public static String token(long userId, String... roles) {
        return construir(userId, List.of(roles), clave(SECRETO), Jwts.SIG.HS256,
                Instant.now().plusSeconds(900));
    }

    public static String tokenHmac512(long userId, String... roles) {
        return construir(userId, List.of(roles), clave(SECRETO), Jwts.SIG.HS512,
                Instant.now().plusSeconds(900));
    }

    public static String tokenSinRoles(long userId) {
        return construir(userId, null, clave(SECRETO), Jwts.SIG.HS256, Instant.now().plusSeconds(900));
    }

    public static String tokenConRolesNoTexto(long userId) {
        return construir(userId, List.of(123, "INVERSIONISTA"), clave(SECRETO), Jwts.SIG.HS256,
                Instant.now().plusSeconds(900));
    }

    public static String tokenConSubject(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(clave(SECRETO), Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenExpirado(long userId) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(ahora.minusSeconds(7200)))
                .expiration(Date.from(ahora.minusSeconds(3600)))
                .signWith(clave(SECRETO), Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenConFirmaDistinta(long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(clave("otro-secreto-de-pruebas-0123456789abcdef"), Jwts.SIG.HS256)
                .compact();
    }

    public static String tokenMalformado() {
        return "esto-no-es-un-jwt";
    }

    private static String construir(long userId, Object roles, SecretKey clave,
                                    SecureDigestAlgorithm<SecretKey, ?> algoritmo,
                                    Instant expiracion) {
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiracion));
        if (roles != null) {
            builder.claim("roles", roles);
        }
        return builder.signWith(clave, algoritmo).compact();
    }

    private static SecretKey clave(String secreto) {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }
}
