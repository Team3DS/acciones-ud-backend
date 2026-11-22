package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SesionCheckoutRepositoryPort {

    Optional<SesionCheckout> findAbiertaVigente(Long userId, Long precioPlanId, Instant ahora);

    List<SesionCheckout> findAbiertasByUserId(Long userId, Instant ahora);

    Optional<SesionCheckout> findByIdempotencyKey(Long userId, String idempotencyKey);

    Optional<SesionCheckout> findByStripeSessionId(String stripeSessionId);

    SesionCheckout save(SesionCheckout sesion);
}
