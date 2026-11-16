package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

@Component
public class JwtTokenValidator {
    private final SecretKey key;

    public JwtTokenValidator(JwtProperties properties) {
        key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public long validar(String token) {
        return autenticar(token).id();
    }

    public UsuarioAutenticado autenticar(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (claims.getExpiration() == null) throw new JwtException("El token requiere expiración.");
        try {
            long id = Long.parseLong(claims.getSubject());
            if (id <= 0) throw new JwtException("Identidad inválida.");
            return new UsuarioAutenticado(id, roles(claims.get("roles")));
        } catch (NumberFormatException exception) {
            throw new JwtException("Identidad inválida.");
        }
    }

    private static Set<String> roles(Object claim) {
        if (claim == null) return Set.of();
        if (!(claim instanceof Collection<?> values) || values.size() > 32) {
            throw new JwtException("Roles inválidos.");
        }
        Set<String> roles = new HashSet<>();
        for (Object value : values) {
            if (!(value instanceof String role)
                    || !role.matches("[A-Z][A-Z0-9_]{0,63}")
                    || role.equals("ROLE_")) {
                throw new JwtException("Roles inválidos.");
            }
            roles.add(role.startsWith("ROLE_") ? role : "ROLE_" + role);
        }
        return Set.copyOf(roles);
    }
}
