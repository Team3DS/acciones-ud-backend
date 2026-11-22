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
import java.util.stream.Collectors;

@Component
public class VerificadorJwt {

    private static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO_ROL = "ROLE_";

    private final SecretKey llave;

    public VerificadorJwt(JwtProperties propiedades) {
        this.llave = Keys.hmacShaKeyFor(propiedades.secret().getBytes(StandardCharsets.UTF_8));
    }

    public UsuarioAutenticado validar(String token) {
        Claims datos = leerFirma(token);
        comprobarVigencia(datos);
        return new UsuarioAutenticado(leerIdentidad(datos), convertirRoles(datos.get(CLAIM_ROLES)));
    }

    private Claims leerFirma(String token) {
        return Jwts.parser()
                .verifyWith(llave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static void comprobarVigencia(Claims datos) {
        if (datos.getExpiration() == null) {
            throw new JwtException("El token debe declarar una fecha de expiracion");
        }
    }

    private static long leerIdentidad(Claims datos) {
        return convertirIdentidad(datos.getSubject());
    }

    private static long convertirIdentidad(String subject) {
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

    private static Set<String> convertirRoles(Object valor) {
        if (!(valor instanceof Collection<?> elementos)) {
            return Set.of();
        }
        Set<String> roles = new LinkedHashSet<>();
        for (Object elemento : elementos) {
            if (elemento instanceof String rol && !rol.isBlank()) {
                roles.add(quitarPrefijo(rol));
            }
        }
        return Set.copyOf(roles.stream().collect(Collectors.toSet()));
    }

    private static String quitarPrefijo(String rol) {
        return rol.startsWith(PREFIJO_ROL) ? rol.substring(PREFIJO_ROL.length()) : rol;
    }
}
