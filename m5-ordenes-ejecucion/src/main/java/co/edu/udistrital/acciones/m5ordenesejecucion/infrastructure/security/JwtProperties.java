package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "dGhpcy1pcy1hLXNhbXBsZS1qd3Qtc2VjcmV0LWtleS1mb3ItZGV2ZWxvcG1lbnQtdXNlLW9ubHktMjU2LWJpdA==";
        }
    }
}
