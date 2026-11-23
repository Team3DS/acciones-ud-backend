package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Firma los tokens que M5 usa al llamar a otros servicios, con el mismo secreto compartido (JWT_SECRET).
 * <ul>
 *   <li>{@link #paraUsuario(Long)}: reservas en M2 y M6, que solo permiten operar al dueño de la cuenta.</li>
 *   <li>{@link #deSistema()}: consultas a M4 (estado del mercado y cotizaciones).</li>
 * </ul>
 */
@Component
public class EmisorTokenInterno {

    static final Duration VIGENCIA = Duration.ofMinutes(2);
    /** Usuario técnico de M5, con la convención 9000000NN de los demás servicios (M10 usa 900000010). */
    static final String ID_SISTEMA = "900000005";
    private static final String ROL_INVERSIONISTA = "INVERSIONISTA";
    private static final String ROL_SISTEMA = "SISTEMA";

    private final SecretKey clave;

    public EmisorTokenInterno(JwtProperties jwtProperties) {
        this.clave = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String paraUsuario(Long userId) {
        return firmar(String.valueOf(userId), ROL_INVERSIONISTA);
    }

    public String deSistema() {
        return firmar(ID_SISTEMA, ROL_SISTEMA);
    }

    private String firmar(String sujeto, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(sujeto)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(VIGENCIA)))
                .claim("roles", List.of(rol))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }
}
