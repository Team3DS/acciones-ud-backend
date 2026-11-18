package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.m4-client")
public record M4ClientProperties(String baseUrl, int timeoutSeconds, int cacheTtlSeconds) {

    public M4ClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.m4-client.base-url es obligatorio");
        }
        if (timeoutSeconds <= 0 || cacheTtlSeconds <= 0) {
            throw new IllegalStateException("Los tiempos del cliente M4 deben ser positivos");
        }
    }
}
