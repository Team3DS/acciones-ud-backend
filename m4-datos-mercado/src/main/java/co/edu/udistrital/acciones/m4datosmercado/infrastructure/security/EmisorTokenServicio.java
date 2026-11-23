package co.edu.udistrital.acciones.m4datosmercado.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/** Token de sistema con el que M4 consulta a M3 (validación de símbolos y horarios), firmado con JWT_SECRET. */
@Component
public class EmisorTokenServicio {

    /** Usuario técnico de M4, con la convención 9000000NN de los demás servicios (M10 usa 900000010). */
    static final String ID_SISTEMA = "900000004";
    static final Duration VIGENCIA = Duration.ofMinutes(5);
    private static final Duration MARGEN_RENOVACION = Duration.ofSeconds(30);

    private final SecretKey clave;
    private String token;
    private Instant vencimiento = Instant.EPOCH;

    public EmisorTokenServicio(JwtProperties properties) {
        this.clave = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public synchronized String obtenerToken() {
        Instant ahora = Instant.now();
        if (token == null || !ahora.isBefore(vencimiento.minus(MARGEN_RENOVACION))) {
            vencimiento = ahora.plus(VIGENCIA);
            token = Jwts.builder()
                    .subject(ID_SISTEMA)
                    .issuedAt(Date.from(ahora))
                    .expiration(Date.from(vencimiento))
                    .claim("roles", List.of("SISTEMA"))
                    .signWith(clave, Jwts.SIG.HS256)
                    .compact();
        }
        return token;
    }
}
