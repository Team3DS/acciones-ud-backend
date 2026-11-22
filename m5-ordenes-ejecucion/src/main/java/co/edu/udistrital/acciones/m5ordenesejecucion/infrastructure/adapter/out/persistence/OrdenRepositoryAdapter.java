package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrdenRepositoryAdapter implements OrdenRepositoryPort {

    /** Estados en los que la orden está en el broker y puede ejecutarse total o parcialmente. */
    private static final List<EstadoOrden> ESTADOS_EN_BROKER = List.of(
            EstadoOrden.ENVIADA, EstadoOrden.FIRMADA_COMISIONISTA,
            EstadoOrden.EN_EJECUCION, EstadoOrden.PARCIALMENTE_EJECUTADA);

    private final OrdenJpaRepository jpaRepository;

    public OrdenRepositoryAdapter(OrdenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Orden> findById(Long id) {
        return jpaRepository.findById(id).map(OrdenEntity::toDomain);
    }

    @Override
    public Optional<Orden> findByClientOrderId(UUID clientOrderId) {
        return jpaRepository.findByClientOrderId(clientOrderId).map(OrdenEntity::toDomain);
    }

    @Override
    public Optional<Orden> findByBrokerOrderId(String brokerOrderId) {
        return jpaRepository.findByBrokerOrderId(brokerOrderId).map(OrdenEntity::toDomain);
    }

    @Override
    public List<Orden> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(OrdenEntity::toDomain)
                .toList();
    }

    @Override
    public List<Orden> findByUsuarioIdAndFilters(Long usuarioId, EstadoOrden estado, String ticker) {
        return jpaRepository.findByUsuarioIdAndFilters(usuarioId, estado, ticker).stream()
                .map(OrdenEntity::toDomain)
                .toList();
    }

    @Override
    public List<Orden> findEncoladasListasParaEnvio() {
        return jpaRepository.findEncoladasConfirmadas().stream()
                .map(OrdenEntity::toDomain)
                .toList();
    }

    @Override
    public List<Orden> findPendientesDeEjecucion() {
        return jpaRepository.findByEstadoOrdenInAndBrokerOrderIdIsNotNull(ESTADOS_EN_BROKER).stream()
                .map(OrdenEntity::toDomain)
                .toList();
    }

    @Override
    public Orden save(Orden orden) {
        OrdenEntity entity = OrdenEntity.fromDomain(orden);
        OrdenEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
}
