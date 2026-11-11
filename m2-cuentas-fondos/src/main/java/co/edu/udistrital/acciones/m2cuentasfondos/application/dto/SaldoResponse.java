package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.math.BigDecimal;

public record SaldoResponse(
        Long userId,
        BigDecimal available,
        BigDecimal reserved,
        BigDecimal total,
        String currency) {
}
