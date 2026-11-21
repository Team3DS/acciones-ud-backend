package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EjecucionOrdenRepositoryPort {
    Optional<EjecucionOrden> findById(Long id);
    Optional<EjecucionOrden> findByBrokerExecutionId(String brokerExecutionId);
    List<EjecucionOrden> findByOrdenId(Long ordenId);
    List<EjecucionOrden> findByUsuarioIdAndPeriodo(Long usuarioId, Instant desde, Instant hasta);
    EjecucionOrden save(EjecucionOrden ejecucion);
}
