package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;

public record MovimientoCreadoPayload(
        Long movementId,
        Long userId,
        String type,
        BigDecimal amount,
        BigDecimal balance) {
}
