package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret) {
}
