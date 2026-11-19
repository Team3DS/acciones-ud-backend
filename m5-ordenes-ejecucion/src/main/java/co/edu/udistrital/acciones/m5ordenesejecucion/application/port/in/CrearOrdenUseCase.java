package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import java.math.BigDecimal;

public interface CrearOrdenUseCase {
    Orden ejecutar(CrearOrdenCommand comando);

    OrdenResultado convertir(Orden orden);

    record CrearOrdenCommand(
            Long usuarioId,
            String simbolo,
            String lado,
            String tipo,
            Integer cantidad,
            BigDecimal precio,
            String idempotencyKey
    ) {}

    record OrdenResultado(
            Long id,
            String simbolo,
            String lado,
            String tipo,
            Integer cantidad,
            BigDecimal precio,
            BigDecimal comision,
            BigDecimal valorTotal,
            String estado,
            String clientOrderId
    ) {}
}
