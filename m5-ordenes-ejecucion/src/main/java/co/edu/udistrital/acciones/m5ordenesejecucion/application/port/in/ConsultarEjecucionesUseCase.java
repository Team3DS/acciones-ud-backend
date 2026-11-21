package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;

import java.math.BigDecimal;
import java.time.Instant;

public interface ConsultarEjecucionesUseCase {
    EjecucionOrden buscarPorId(Long executionId);
    ResumenEjecucionesResponse resumen(Long usuarioId, Instant desde, Instant hasta);

    record ResumenEjecucionesResponse(
            Long usuarioId,
            Instant desde,
            Instant hasta,
            BigDecimal gananciaBrutaRealizada,
            BigDecimal comisionesCompra,
            BigDecimal comisionesVenta,
            int ventasPendientesConfirmacion
    ) {}
}
