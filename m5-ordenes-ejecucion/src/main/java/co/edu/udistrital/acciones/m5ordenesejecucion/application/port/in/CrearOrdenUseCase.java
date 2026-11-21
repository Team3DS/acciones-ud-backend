package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;

import java.math.BigDecimal;
import java.util.UUID;

public interface CrearOrdenUseCase {
    Orden ejecutar(CrearOrdenCommand comando);

    record CrearOrdenCommand(
            Long usuarioId,
            Long accionId,
            String ticker,
            TipoOrden tipoOrden,
            LadoOrden ladoOrden,
            VigenciaOrden vigenciaOrden,
            Integer cantidad,
            BigDecimal precioLimite,
            BigDecimal precioActivacion,
            BigDecimal precioEstimado,
            UUID clientOrderId
    ) {}
}
