package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortafolioResponse(
        Long userId,
        List<PosicionResponse> positions,
        BigDecimal totalValue,
        BigDecimal totalCost,
        BigDecimal unrealizedPnl,
        BigDecimal realizedPnl,
        String currency,
        Instant valuedAt,
        boolean stale) {
}
