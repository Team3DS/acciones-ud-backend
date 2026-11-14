package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;

public record FondosReservadosPayload(
        Long reservationId,
        String orderId,
        Long userId,
        BigDecimal amount) {
}
