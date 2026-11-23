package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdenJpaRepository extends JpaRepository<OrdenEntity, Long> {
    Optional<OrdenEntity> findByClientOrderId(UUID clientOrderId);
    Optional<OrdenEntity> findByBrokerOrderId(String brokerOrderId);
    List<OrdenEntity> findByUsuarioId(Long usuarioId);

    @Query("SELECT o FROM OrdenEntity o WHERE o.usuarioId = :usuarioId " +
            "AND (:estado IS NULL OR o.estadoOrden = :estado) " +
            "AND (:ticker IS NULL OR o.ticker = :ticker) " +
            "ORDER BY o.creadaEn DESC")
    List<OrdenEntity> findByUsuarioIdAndFilters(@Param("usuarioId") Long usuarioId,
                                                @Param("estado") EstadoOrden estado,
                                                @Param("ticker") String ticker);

    @Query("SELECT o FROM OrdenEntity o WHERE o.estadoOrden = 'PENDIENTE' " +
            "AND o.requiereEncolamiento = true AND o.confirmadoEncolamientoEn IS NOT NULL")
    List<OrdenEntity> findEncoladasConfirmadas();

    List<OrdenEntity> findByEstadoOrdenInAndBrokerOrderIdIsNotNull(Collection<EstadoOrden> estados);
}
