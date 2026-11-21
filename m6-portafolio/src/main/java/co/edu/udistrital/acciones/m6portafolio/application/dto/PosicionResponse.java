package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PosicionResponse(
        Long userId,
        String symbol,
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        BigDecimal unrealizedPnlPct,
        BigDecimal realizedPnl,
        Instant priceTimestamp,
        boolean stale,
        String currency) {
}
