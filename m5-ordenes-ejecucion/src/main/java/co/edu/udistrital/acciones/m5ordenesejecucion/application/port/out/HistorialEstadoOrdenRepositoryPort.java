package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;

import java.util.List;

public interface HistorialEstadoOrdenRepositoryPort {
    List<HistorialEstadoOrden> findByOrdenId(Long ordenId);
    HistorialEstadoOrden save(HistorialEstadoOrden historial);
}
