package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record Cotizacion(
        String symbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePct,
        Instant timestamp) {
}
