package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConfirmacionEjecucionRepositoryAdapter implements ConfirmacionEjecucionRepositoryPort {

    private final ConfirmacionEjecucionJpaRepository jpaRepository;

    public ConfirmacionEjecucionRepositoryAdapter(ConfirmacionEjecucionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ConfirmacionEjecucion> findByEjecucionIdAndServicio(Long ejecucionOrdenId, ServicioParticipante servicio) {
        return jpaRepository.findById(new ConfirmacionEjecucionId(ejecucionOrdenId, servicio))
                .map(ConfirmacionEjecucionEntity::toDomain);
    }

    @Override
    public List<ConfirmacionEjecucion> findByEjecucionId(Long ejecucionOrdenId) {
        return jpaRepository.findByEjecucionOrdenId(ejecucionOrdenId).stream()
                .map(ConfirmacionEjecucionEntity::toDomain)
                .toList();
    }

    @Override
    public ConfirmacionEjecucion save(ConfirmacionEjecucion confirmacion) {
        ConfirmacionEjecucionEntity entity = ConfirmacionEjecucionEntity.fromDomain(confirmacion);
        return jpaRepository.save(entity).toDomain();
    }
}
