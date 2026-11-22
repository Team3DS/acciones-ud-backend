package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

@Repository
interface SpringDataSuscripcionRepository extends JpaRepository<Suscripcion, Long> {

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<Suscripcion> findFirstByUserIdAndEstadoInOrderByPeriodoFinDesc(
            Long userId, Collection<EstadoSuscripcion> estados);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<Suscripcion> findFirstByUserIdOrderByCreadoEnDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<Suscripcion> findByStripeSubscriptionId(String stripeSubscriptionId);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    @Query("select s from Suscripcion s where s.id = :id")
    Optional<Suscripcion> findConPrecioPorId(@Param("id") Long id);
}

@Repository
public class JpaSuscripcionRepositoryAdapter implements SuscripcionRepositoryPort {

    private static final EnumSet<EstadoSuscripcion> VIGENTES = EnumSet.of(
            EstadoSuscripcion.INCOMPLETA,
            EstadoSuscripcion.ACTIVA,
            EstadoSuscripcion.EN_MORA);

    private final SpringDataSuscripcionRepository repositorio;

    public JpaSuscripcionRepositoryAdapter(SpringDataSuscripcionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<Suscripcion> findVigenteByUserId(Long userId) {
        return repositorio.findFirstByUserIdAndEstadoInOrderByPeriodoFinDesc(userId, VIGENTES);
    }

    @Override
    public Optional<Suscripcion> findUltimaByUserId(Long userId) {
        return repositorio.findFirstByUserIdOrderByCreadoEnDescIdDesc(userId);
    }

    @Override
    public Optional<Suscripcion> findByStripeSubscriptionId(String stripeSubscriptionId) {
        return repositorio.findByStripeSubscriptionId(stripeSubscriptionId);
    }

    @Override
    public Optional<Suscripcion> findById(Long id) {
        return repositorio.findConPrecioPorId(id);
    }

    @Override
    public Suscripcion save(Suscripcion suscripcion) {
        return repositorio.save(suscripcion);
    }
}
