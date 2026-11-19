package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdenJpaRepository extends JpaRepository<OrdenEntity, Long> {
    Optional<OrdenEntity> findByClientOrderId(String clientOrderId);
    List<OrdenEntity> findByUsuarioId(Long usuarioId);
    List<OrdenEntity> findByEstado(EstadoOrden estado);
    List<OrdenEntity> findByUsuarioIdAndEstado(Long usuarioId, EstadoOrden estado);
    boolean existsByClientOrderId(String clientOrderId);
}
