package co.edu.udistrital.acciones.m6portafolio.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class TokenServicioProvider {

    private static final long MARGEN_RENOVACION_SEGUNDOS = 10;

    private final SecretKey clave;
    private final ServicioProperties propiedades;
    private final Clock clock;
    private String token;
    private Instant expiraEn;

    public TokenServicioProvider(JwtProperties jwtProperties, ServicioProperties propiedades, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public synchronized String obtenerToken() {
        Instant ahora = Instant.now(clock);
        if (token == null || !ahora.isBefore(expiraEn.minusSeconds(MARGEN_RENOVACION_SEGUNDOS))) {
            expiraEn = ahora.plus(propiedades.tokenTtl());
            token = Jwts.builder()
                    .subject(String.valueOf(propiedades.usuarioId()))
                    .issuedAt(Date.from(ahora))
                    .expiration(Date.from(expiraEn))
                    .claim("roles", List.of("SISTEMA"))
                    .signWith(clave, Jwts.SIG.HS256)
                    .compact();
        }
        return token;
    }
}
