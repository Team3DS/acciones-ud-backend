package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.clientes-http")
public record ClientesHttpProperties(
        String m3BaseUrl,
        String m4BaseUrl,
        int timeoutSeconds,
        int cacheTtlSeconds) {

    public ClientesHttpProperties {
        if (m3BaseUrl == null || m3BaseUrl.isBlank()) {
            throw new IllegalStateException("app.clientes-http.m3-base-url es obligatorio");
        }
        if (m4BaseUrl == null || m4BaseUrl.isBlank()) {
            throw new IllegalStateException("app.clientes-http.m4-base-url es obligatorio");
        }
        if (timeoutSeconds <= 0 || cacheTtlSeconds <= 0) {
            throw new IllegalStateException("Los tiempos de los clientes HTTP deben ser positivos");
        }
    }
}
