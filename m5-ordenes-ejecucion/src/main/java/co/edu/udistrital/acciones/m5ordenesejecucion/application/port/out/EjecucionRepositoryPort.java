package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;

import java.util.List;
import java.util.Optional;

public interface EjecucionRepositoryPort {
    Optional<EjecucionOrden> findById(Long id);
    Optional<EjecucionOrden> findByExecutionId(String executionId);
    EjecucionOrden save(EjecucionOrden ejecucion);
    List<EjecucionOrden> findByOrdenId(Long ordenId);
    boolean existsByExecutionId(String executionId);
}
