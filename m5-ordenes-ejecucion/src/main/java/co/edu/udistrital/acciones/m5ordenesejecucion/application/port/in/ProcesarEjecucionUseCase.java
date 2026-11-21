package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;

import java.math.BigDecimal;
import java.time.Instant;

public interface ProcesarEjecucionUseCase {
    EjecucionOrden ejecutar(ProcesarEjecucionCommand comando);

    record ProcesarEjecucionCommand(
            Long ordenId,
            String brokerExecutionId,
            String brokerOrderId,
            Integer cantidad,
            BigDecimal precioEjecucion,
            BigDecimal comision,
            Instant ejecutadaEn
    ) {}
}
