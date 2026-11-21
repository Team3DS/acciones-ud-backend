package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import java.util.List;

public interface ConsultarOrdenUseCase {
    Orden buscarPorId(Long id);
    List<Orden> buscarPorUsuario(Long usuarioId, EstadoOrden estado, String ticker);
    List<EjecucionOrden> buscarFillsPorOrden(Long ordenId);
    List<HistorialEstadoOrden> buscarHistorialPorOrden(Long ordenId);
}
