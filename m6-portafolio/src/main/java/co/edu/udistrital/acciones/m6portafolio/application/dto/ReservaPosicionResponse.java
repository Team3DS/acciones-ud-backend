package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.time.Instant;

public record ReservaPosicionResponse(
        Long reservationId,
        Long orderId,
        Long userId,
        String symbol,
        Integer quantity,
        Integer executedQuantity,
        String status,
        String reason,
        Instant createdAt,
        Instant releasedAt) {
}