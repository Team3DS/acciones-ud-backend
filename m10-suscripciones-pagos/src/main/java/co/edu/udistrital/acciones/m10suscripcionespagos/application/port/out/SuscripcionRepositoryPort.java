package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;

import java.util.Optional;

public interface SuscripcionRepositoryPort {

    Optional<Suscripcion> findVigenteByUserId(Long userId);

    Optional<Suscripcion> findUltimaByUserId(Long userId);

    Optional<Suscripcion> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Suscripcion> findById(Long id);

    Suscripcion save(Suscripcion suscripcion);
}
