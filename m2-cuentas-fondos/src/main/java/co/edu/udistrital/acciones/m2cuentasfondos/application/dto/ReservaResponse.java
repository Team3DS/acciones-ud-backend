package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservaResponse(
        Long reservationId,
        String orderId,
        Long userId,
        BigDecimal amount,
        String status,
        BigDecimal available,
        BigDecimal reserved,
        BigDecimal total,
        String currency,
        Instant createdAt) {
}
