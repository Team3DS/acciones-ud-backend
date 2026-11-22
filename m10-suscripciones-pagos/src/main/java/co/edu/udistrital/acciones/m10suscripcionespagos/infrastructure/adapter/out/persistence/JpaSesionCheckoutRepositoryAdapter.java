package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSesionCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataSesionCheckoutRepository extends JpaRepository<SesionCheckout, Long> {

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<SesionCheckout> findFirstByUserIdAndPrecioPlanIdAndEstadoAndExpiraEnAfterOrderByCreadoEnDesc(
            Long userId, Long precioPlanId, EstadoSesionCheckout estado, Instant ahora);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    List<SesionCheckout> findByUserIdAndEstadoAndExpiraEnAfterOrderByCreadoEnDesc(
            Long userId, EstadoSesionCheckout estado, Instant ahora);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<SesionCheckout> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    @EntityGraph(attributePaths = {"precioPlan", "precioPlan.plan"})
    Optional<SesionCheckout> findByStripeSessionId(String stripeSessionId);

    boolean existsByUserId(Long userId);
}

@Repository
public class JpaSesionCheckoutRepositoryAdapter implements SesionCheckoutRepositoryPort {

    private final SpringDataSesionCheckoutRepository repositorio;

    public JpaSesionCheckoutRepositoryAdapter(SpringDataSesionCheckoutRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<SesionCheckout> findAbiertaVigente(Long userId, Long precioPlanId, Instant ahora) {
        return repositorio.findFirstByUserIdAndPrecioPlanIdAndEstadoAndExpiraEnAfterOrderByCreadoEnDesc(
                userId, precioPlanId, EstadoSesionCheckout.ABIERTA, ahora);
    }

    @Override
    public List<SesionCheckout> findAbiertasByUserId(Long userId, Instant ahora) {
        return repositorio.findByUserIdAndEstadoAndExpiraEnAfterOrderByCreadoEnDesc(
                userId, EstadoSesionCheckout.ABIERTA, ahora);
    }

    @Override
    public Optional<SesionCheckout> findByIdempotencyKey(Long userId, String idempotencyKey) {
        return repositorio.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    @Override
    public Optional<SesionCheckout> findByStripeSessionId(String stripeSessionId) {
        return repositorio.findByStripeSessionId(stripeSessionId);
    }

    @Override
    public boolean existePorUserId(Long userId) {
        return repositorio.existsByUserId(userId);
    }

    @Override
    public SesionCheckout save(SesionCheckout sesion) {
        return repositorio.save(sesion);
    }
}
