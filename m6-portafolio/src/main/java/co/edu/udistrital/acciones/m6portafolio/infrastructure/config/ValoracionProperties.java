package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.valoracion")
public record ValoracionProperties(
        boolean schedulerEnabled,
        int intervalSeconds,
        boolean soloMercadoAbierto,
        int lote) {

    public ValoracionProperties {
        if (intervalSeconds <= 0) {
            throw new IllegalStateException("app.valoracion.interval-seconds debe ser positivo");
        }
        if (lote <= 0) {
            throw new IllegalStateException("app.valoracion.lote debe ser positivo");
        }
    }
}
