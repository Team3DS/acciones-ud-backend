package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OutboxEventoRepositoryAdapter implements OutboxEventoRepositoryPort {

    private final OutboxEventoJpaRepository jpaRepository;

    public OutboxEventoRepositoryAdapter(OutboxEventoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<OutboxEvento> findById(UUID id) {
        return jpaRepository.findById(id).map(OutboxEventoEntity::toDomain);
    }

    @Override
    public List<OutboxEvento> findPendientes(int limite) {
        return jpaRepository.findPendientes(PageRequest.of(0, limite)).stream()
                .map(OutboxEventoEntity::toDomain)
                .toList();
    }

    @Override
    public OutboxEvento save(OutboxEvento evento) {
        OutboxEventoEntity entity = OutboxEventoEntity.fromDomain(evento);
        return jpaRepository.save(entity).toDomain();
    }
}
