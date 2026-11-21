package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class EjecucionOrdenRepositoryAdapter implements EjecucionOrdenRepositoryPort {

    private final EjecucionOrdenJpaRepository jpaRepository;

    public EjecucionOrdenRepositoryAdapter(EjecucionOrdenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<EjecucionOrden> findById(Long id) {
        return jpaRepository.findById(id).map(EjecucionOrdenEntity::toDomain);
    }

    @Override
    public Optional<EjecucionOrden> findByBrokerExecutionId(String brokerExecutionId) {
        return jpaRepository.findByBrokerExecutionId(brokerExecutionId).map(EjecucionOrdenEntity::toDomain);
    }

    @Override
    public List<EjecucionOrden> findByOrdenId(Long ordenId) {
        return jpaRepository.findByOrdenIdOrderByEjecutadaEnAsc(ordenId).stream()
                .map(EjecucionOrdenEntity::toDomain)
                .toList();
    }

    @Override
    public List<EjecucionOrden> findByUsuarioIdAndPeriodo(Long usuarioId, Instant desde, Instant hasta) {
        return jpaRepository.findByUsuarioIdAndPeriodo(usuarioId, desde, hasta).stream()
                .map(EjecucionOrdenEntity::toDomain)
                .toList();
    }

    @Override
    public EjecucionOrden save(EjecucionOrden ejecucion) {
        EjecucionOrdenEntity entity = EjecucionOrdenEntity.fromDomain(ejecucion);
        return jpaRepository.save(entity).toDomain();
    }
}
