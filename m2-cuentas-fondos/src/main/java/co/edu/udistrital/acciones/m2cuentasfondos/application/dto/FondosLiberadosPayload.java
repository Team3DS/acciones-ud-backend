package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;

public record FondosLiberadosPayload(
        Long reservationId,
        String reason,
        BigDecimal amount,
        Long userId,
        String orderId) {
}
