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

    /** Elige únicamente predicados con límites presentes: PostgreSQL no debe inferir el tipo de un Instant nulo. */
    default List<EjecucionOrdenEntity> findByUsuarioIdAndPeriodo(Long usuarioId, Instant desde, Instant hasta) {
        if (desde != null && hasta != null) {
            return findByUsuarioIdEntre(usuarioId, desde, hasta);
        }
        if (desde != null) {
            return findByUsuarioIdDesde(usuarioId, desde);
        }
        if (hasta != null) {
            return findByUsuarioIdHasta(usuarioId, hasta);
        }
        return findByUsuarioIdSinLimites(usuarioId);
    }

    @Query("SELECT e FROM EjecucionOrdenEntity e JOIN OrdenEntity o ON e.ordenId = o.id " +
            "WHERE o.usuarioId = :usuarioId ORDER BY e.ejecutadaEn DESC, e.id DESC")
    List<EjecucionOrdenEntity> findByUsuarioIdSinLimites(@Param("usuarioId") Long usuarioId);

    @Query("SELECT e FROM EjecucionOrdenEntity e JOIN OrdenEntity o ON e.ordenId = o.id " +
            "WHERE o.usuarioId = :usuarioId AND e.ejecutadaEn >= :desde ORDER BY e.ejecutadaEn DESC, e.id DESC")
    List<EjecucionOrdenEntity> findByUsuarioIdDesde(@Param("usuarioId") Long usuarioId, @Param("desde") Instant desde);

    @Query("SELECT e FROM EjecucionOrdenEntity e JOIN OrdenEntity o ON e.ordenId = o.id " +
            "WHERE o.usuarioId = :usuarioId AND e.ejecutadaEn <= :hasta ORDER BY e.ejecutadaEn DESC, e.id DESC")
    List<EjecucionOrdenEntity> findByUsuarioIdHasta(@Param("usuarioId") Long usuarioId, @Param("hasta") Instant hasta);

    @Query("SELECT e FROM EjecucionOrdenEntity e JOIN OrdenEntity o ON e.ordenId = o.id " +
            "WHERE o.usuarioId = :usuarioId AND e.ejecutadaEn >= :desde AND e.ejecutadaEn <= :hasta " +
            "ORDER BY e.ejecutadaEn DESC, e.id DESC")
    List<EjecucionOrdenEntity> findByUsuarioIdEntre(@Param("usuarioId") Long usuarioId,
                                                  @Param("desde") Instant desde, @Param("hasta") Instant hasta);
}
