package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtTokenValidator {

    private static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO_ROL = "ROLE_";

    private final SecretKey clave;

    public JwtTokenValidator(JwtProperties properties) {
        this.clave = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        return new AuthenticatedUser(userId, extraerRoles(claims.get(CLAIM_ROLES)));
    }

    private Set<String> extraerRoles(Object valor) {
        if (!(valor instanceof Collection<?> coleccion)) {
            return Set.of();
        }
        return coleccion.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(this::sinPrefijo)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String sinPrefijo(String rol) {
        return rol.startsWith(PREFIJO_ROL) ? rol.substring(PREFIJO_ROL.length()) : rol;
    }
}
