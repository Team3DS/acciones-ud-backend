package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservaPosicionResponse(
        Long reservationId,
        String orderId,
        Long userId,
        String symbol,
        BigDecimal quantity,
        BigDecimal executedQuantity,
        String status,
        String reason,
        Instant createdAt,
        Instant confirmedAt,
        Instant releasedAt) {
}
