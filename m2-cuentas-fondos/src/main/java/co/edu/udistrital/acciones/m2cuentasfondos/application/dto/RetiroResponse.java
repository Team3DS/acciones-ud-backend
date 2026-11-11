package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RetiroResponse(
        Long movementId,
        Long userId,
        BigDecimal amount,
        BigDecimal available,
        BigDecimal reserved,
        BigDecimal total,
        String currency,
        Instant createdAt) {
}
