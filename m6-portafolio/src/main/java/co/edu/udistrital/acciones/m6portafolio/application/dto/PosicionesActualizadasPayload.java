package co.edu.udistrital.acciones.m6portafolio.application.dto;

import java.math.BigDecimal;

public record PosicionesActualizadasPayload(
        String orderId,
        String executionId,
        Long userId,
        String symbol,
        String side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal newQuantity,
        BigDecimal averageCost,
        BigDecimal realizedPnl) {
}
