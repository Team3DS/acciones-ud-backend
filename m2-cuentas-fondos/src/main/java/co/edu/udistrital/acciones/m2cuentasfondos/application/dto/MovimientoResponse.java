package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimientoResponse(
        Long movementId,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String reference,
        String description,
        Instant createdAt) {
}
