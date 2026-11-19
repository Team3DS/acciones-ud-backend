package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import java.util.List;
import java.util.Optional;

public interface OrdenRepositoryPort {
    Optional<Orden> findById(Long id);
    Optional<Orden> findByClientOrderId(String clientOrderId);
    Orden save(Orden orden);
    List<Orden> findByUsuarioId(Long usuarioId);
    List<Orden> findByEstado(EstadoOrden estado);
    List<Orden> findByUsuarioIdAndEstado(Long usuarioId, EstadoOrden estado);
    boolean existsById(Long id);
}
