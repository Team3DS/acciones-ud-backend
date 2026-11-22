package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.internal")
public record TokenInternoProperties(String token) {

    public static final int CARACTERES_MINIMOS = 32;

    public TokenInternoProperties {
        token = token == null ? "" : token;
        if (!token.isBlank() && token.length() < CARACTERES_MINIMOS) {
            throw new IllegalStateException(
                    "app.internal.token debe tener al menos " + CARACTERES_MINIMOS + " caracteres");
        }
    }

    public boolean configurado() {
        return !token.isBlank();
    }
}
