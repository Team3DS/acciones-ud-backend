package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.webhooks")
public record WebhooksProperties(
        boolean procesadorEnabled,
        Duration fixedDelay,
        int lote,
        int maxIntentos,
        Duration backoffBase,
        Duration backoffMax) {

    private static final Duration DEMORA_POR_DEFECTO = Duration.ofSeconds(10);
    private static final int LOTE_POR_DEFECTO = 50;
    private static final int INTENTOS_POR_DEFECTO = 10;
    private static final Duration BACKOFF_BASE_POR_DEFECTO = Duration.ofSeconds(30);
    private static final Duration BACKOFF_MAX_POR_DEFECTO = Duration.ofHours(6);

    public WebhooksProperties {
        fixedDelay = fixedDelay == null ? DEMORA_POR_DEFECTO : fixedDelay;
        lote = lote <= 0 ? LOTE_POR_DEFECTO : lote;
        maxIntentos = maxIntentos <= 0 ? INTENTOS_POR_DEFECTO : maxIntentos;
        backoffBase = backoffBase == null ? BACKOFF_BASE_POR_DEFECTO : backoffBase;
        backoffMax = backoffMax == null ? BACKOFF_MAX_POR_DEFECTO : backoffMax;
        if (fixedDelay.isNegative() || fixedDelay.isZero()) {
            throw new IllegalStateException("app.webhooks.fixed-delay debe ser positivo");
        }
        if (backoffBase.isNegative() || backoffBase.isZero()) {
            throw new IllegalStateException("app.webhooks.backoff-base debe ser positivo");
        }
        if (backoffMax.compareTo(backoffBase) < 0) {
            throw new IllegalStateException("app.webhooks.backoff-max no puede ser menor que backoff-base");
        }
    }
}
