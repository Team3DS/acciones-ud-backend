package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

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
public class EmisorTokenServicio {

    private static final long MARGEN_RENOVACION_SEGUNDOS = 10;

    private final SecretKey clave;
    private final ServicioProperties ajustes;
    private final Clock reloj;
    private String token;
    private Instant vencimiento;

    public EmisorTokenServicio(JwtProperties jwtProperties, ServicioProperties propiedades, Clock clock) {
        this.clave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.ajustes = propiedades;
        this.reloj = clock;
    }

    public synchronized String obtenerToken() {
        if (necesitaOtroToken()) {
            token = firmar();
        }
        return token;
    }

    private boolean necesitaOtroToken() {
        Instant ahora = Instant.now(reloj);
        if (token != null && ahora.isBefore(vencimiento.minusSeconds(MARGEN_RENOVACION_SEGUNDOS))) {
            return false;
        }
        vencimiento = ahora.plus(ajustes.tokenTtl());
        return true;
    }

    private String firmar() {
        Instant ahora = Instant.now(reloj);
        return Jwts.builder()
                .subject(String.valueOf(ajustes.usuarioId()))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vencimiento))
                .claim("roles", List.of(UsuarioAutenticado.ROL_SISTEMA))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
