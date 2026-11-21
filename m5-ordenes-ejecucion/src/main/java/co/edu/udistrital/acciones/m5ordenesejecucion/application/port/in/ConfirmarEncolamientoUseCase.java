package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

public interface ConfirmarEncolamientoUseCase {
    Orden ejecutar(Long ordenId, Long usuarioId);
}
