package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    public static final int LONGITUD_MINIMA_SECRETO = 32;

    private String secret;

    @PostConstruct
    public void validar() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "app.jwt.secret es obligatorio y debe tener al menos " + LONGITUD_MINIMA_SECRETO + " bytes");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
