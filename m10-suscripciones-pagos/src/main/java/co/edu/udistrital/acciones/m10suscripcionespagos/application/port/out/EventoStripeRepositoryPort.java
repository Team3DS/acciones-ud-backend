package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventoStripeRepositoryPort {

    boolean registrarSiNuevo(EventoStripe evento);

    List<String> findPendientes(Instant ahora, int maxIntentos, int limite);

    Optional<EventoStripe> bloquearParaProcesar(String id, Instant ahora, int maxIntentos);

    Optional<EventoStripe> findById(String id);

    EventoStripe save(EventoStripe evento);
}
