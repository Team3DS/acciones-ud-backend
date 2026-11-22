package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.servicio")
public record ServicioProperties(long usuarioId, Duration tokenTtl) {

    public ServicioProperties {
        if (usuarioId <= 0) {
            throw new IllegalStateException("app.servicio.usuario-id debe ser positivo");
        }
        if (tokenTtl == null || tokenTtl.isNegative() || tokenTtl.isZero()) {
            throw new IllegalStateException("app.servicio.token-ttl debe ser positivo");
        }
    }
}
