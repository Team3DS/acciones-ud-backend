package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

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
            java.util.Set<String> roles = new java.util.HashSet<>();
            if (claims.get("roles") instanceof java.util.Collection<?> values) {
                for (Object value : values)
                    if (value instanceof String role)
                        roles.add(role.startsWith("ROLE_") ? role : "ROLE_" + role);
            }
            return new UsuarioAutenticado(id, java.util.Set.copyOf(roles));
        } catch (NumberFormatException exception) {
            throw new JwtException("Identidad inválida.");
        }
    }
}
