package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class OrdenRepositoryAdapter implements OrdenRepositoryPort {

    private final OrdenJpaRepository jpaRepository;

    public OrdenRepositoryAdapter(OrdenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Orden> findById(Long id) {
        return jpaRepository.findById(id).map(OrdenEntity::toDomain);
    }

    @Override
    public Optional<Orden> findByClientOrderId(String clientOrderId) {
        return jpaRepository.findByClientOrderId(clientOrderId).map(OrdenEntity::toDomain);
    }

    @Override
    public Orden save(Orden orden) {
        OrdenEntity entity = new OrdenEntity(orden);
        return jpaRepository.save(entity).toDomain();
    }

    @Override
    public List<Orden> findByUsuarioId(Long usuarioId) {
        return jpaRepository.findByUsuarioId(usuarioId).stream()
                .map(OrdenEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Orden> findByEstado(EstadoOrden estado) {
        return jpaRepository.findByEstado(estado).stream()
                .map(OrdenEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Orden> findByUsuarioIdAndEstado(Long usuarioId, EstadoOrden estado) {
        return jpaRepository.findByUsuarioIdAndEstado(usuarioId, estado).stream()
                .map(OrdenEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
