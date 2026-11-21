package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PosicionValoradaPayload(
        Long userId,
        String symbol,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        Instant valuedAt) {
}
