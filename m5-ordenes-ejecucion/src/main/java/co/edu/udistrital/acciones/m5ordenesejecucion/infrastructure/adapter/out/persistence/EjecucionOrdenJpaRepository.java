package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EjecucionOrdenJpaRepository extends JpaRepository<EjecucionOrdenEntity, Long> {
    Optional<EjecucionOrdenEntity> findByBrokerExecutionId(String brokerExecutionId);
    List<EjecucionOrdenEntity> findByOrdenIdOrderByEjecutadaEnAsc(Long ordenId);

    @Query("SELECT e FROM EjecucionOrdenEntity e JOIN OrdenEntity o ON e.ordenId = o.id " +
            "WHERE o.usuarioId = :usuarioId " +
            "AND (:desde IS NULL OR e.ejecutadaEn >= :desde) " +
            "AND (:hasta IS NULL OR e.ejecutadaEn <= :hasta) " +
            "ORDER BY e.ejecutadaEn DESC")
    List<EjecucionOrdenEntity> findByUsuarioIdAndPeriodo(@Param("usuarioId") Long usuarioId,
                                                          @Param("desde") Instant desde,
                                                          @Param("hasta") Instant hasta);
}
