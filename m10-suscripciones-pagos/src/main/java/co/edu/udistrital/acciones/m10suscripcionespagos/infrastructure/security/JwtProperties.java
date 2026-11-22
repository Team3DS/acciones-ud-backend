package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret) {

    public static final int BYTES_MINIMOS = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS) {
            throw new IllegalStateException(
                    "app.jwt.secret es obligatorio y debe tener al menos " + BYTES_MINIMOS + " bytes");
        }
    }
}
