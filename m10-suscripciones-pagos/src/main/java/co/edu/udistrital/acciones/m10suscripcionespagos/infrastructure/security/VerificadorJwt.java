package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class VerificadorJwt {

    private static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO_ROL = "ROLE_";

    private final SecretKey clave;

    public VerificadorJwt(JwtProperties propiedades) {
        this.clave = Keys.hmacShaKeyFor(propiedades.secret().getBytes(StandardCharsets.UTF_8));
    }

    public UsuarioAutenticado validar(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        exigirExpiracion(claims);
        return new UsuarioAutenticado(identidad(claims.getSubject()), rolesDe(claims.get(CLAIM_ROLES)));
    }

    private static void exigirExpiracion(Claims claims) {
        if (claims.getExpiration() == null) {
            throw new JwtException("El token debe declarar una fecha de expiracion");
        }
    }

    private static long identidad(String subject) {
        long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            throw new JwtException("La identidad del token no es numerica", ex);
        }
        if (userId <= 0) {
            throw new JwtException("La identidad del token debe ser positiva");
        }
        return userId;
    }

    private static Set<String> rolesDe(Object valor) {
        if (!(valor instanceof Collection<?> elementos)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object elemento : elementos) {
            if (elemento instanceof String rol && !rol.isBlank()) {
                roles.add(rol.startsWith(PREFIJO_ROL) ? rol.substring(PREFIJO_ROL.length()) : rol);
            }
        }
        return Set.copyOf(roles);
    }
}
