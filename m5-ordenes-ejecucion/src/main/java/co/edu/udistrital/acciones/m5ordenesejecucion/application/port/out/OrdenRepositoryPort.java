package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdenRepositoryPort {
    Optional<Orden> findById(Long id);
    Optional<Orden> findByClientOrderId(UUID clientOrderId);
    Optional<Orden> findByBrokerOrderId(String brokerOrderId);
    List<Orden> findByUsuarioId(Long usuarioId);
    List<Orden> findByUsuarioIdAndFilters(Long usuarioId, EstadoOrden estado, String ticker);
    List<Orden> findEncoladasListasParaEnvio();
    Orden save(Orden orden);
}
