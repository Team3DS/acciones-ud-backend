package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HistorialEstadoOrdenRepositoryAdapter implements HistorialEstadoOrdenRepositoryPort {

    private final HistorialEstadoOrdenJpaRepository jpaRepository;

    public HistorialEstadoOrdenRepositoryAdapter(HistorialEstadoOrdenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<HistorialEstadoOrden> findByOrdenId(Long ordenId) {
        return jpaRepository.findByOrdenIdOrderByOcurridoEnAsc(ordenId).stream()
                .map(HistorialEstadoOrdenEntity::toDomain)
                .toList();
    }

    @Override
    public HistorialEstadoOrden save(HistorialEstadoOrden historial) {
        HistorialEstadoOrdenEntity entity = HistorialEstadoOrdenEntity.fromDomain(historial);
        return jpaRepository.save(entity).toDomain();
    }
}
