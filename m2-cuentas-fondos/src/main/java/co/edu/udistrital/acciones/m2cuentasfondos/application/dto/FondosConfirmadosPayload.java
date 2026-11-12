package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;

public record FondosConfirmadosPayload(
        Long reservationId,
        String executionId,
        BigDecimal amount) {
}
