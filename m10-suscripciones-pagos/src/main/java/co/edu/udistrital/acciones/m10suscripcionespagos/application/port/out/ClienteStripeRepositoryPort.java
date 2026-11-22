package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;

import java.util.Optional;

public interface ClienteStripeRepositoryPort {

    Optional<ClienteStripe> findById(Long userId);

    ClienteStripe save(ClienteStripe cliente);
}
