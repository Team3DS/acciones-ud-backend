package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;

import java.time.Instant;

public record FiltroMovimientos(
        TipoMovimiento tipo,
        Instant desde,
        Instant hasta) {
}
