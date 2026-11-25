package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import co.edu.udistrital.acciones.m1identidad.domain.Rol;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out.UsuarioRolJpaRepository;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTtlSeconds;
    private final UsuarioRolJpaRepository roles;

    public JwtService(JwtProperties props) {
        this(props, null);
    }

    @Autowired
    public JwtService(JwtProperties props, UsuarioRolJpaRepository roles) {
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = props.getAccessTtlSeconds();
        this.roles = roles;
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claim("roles", rolesDe(userId))
                .signWith(key)
                .compact();
    }

    /**
     * Roles que leen los demás servicios (claim {@code roles}). Todo usuario es INVERSIONISTA; los demás roles
     * se asignan en la tabla usuario_rol (por ejemplo, ADMINISTRADOR para registrar depósitos en M2).
     */
    public List<String> rolesDe(Long userId) {
        Set<String> asignados = new TreeSet<>();
        asignados.add(Rol.INVERSIONISTA.name());
        if (roles != null) {
            asignados.addAll(roles.findRolesByUsuarioId(userId));
        }
        return List.copyOf(asignados);
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    public boolean isNotificationServiceToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return "900000008".equals(claims.getSubject())
                && "m8-notificaciones".equals(claims.getIssuer())
                && claims.getExpiration() != null
                && claims.get("roles") instanceof List<?> serviceRoles
                && serviceRoles.contains("SISTEMA");
    }
}
