package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ManejadorEventoStripePort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@Component
public class CheckoutSessionManejador implements ManejadorEventoStripePort {

    private static final Logger log = LoggerFactory.getLogger(CheckoutSessionManejador.class);
    private static final String COMPLETADA = "checkout.session.completed";
    private static final String EXPIRADA = "checkout.session.expired";
    private static final Set<String> TIPOS = Set.of(COMPLETADA, EXPIRADA);

    private final PaymentProviderPort proveedor;
    private final SesionCheckoutRepositoryPort sesiones;
    private final ClienteStripeRepositoryPort clientes;
    private final SincronizadorSuscripciones sincronizador;
    private final Clock clock;

    public CheckoutSessionManejador(PaymentProviderPort proveedor,
                                    SesionCheckoutRepositoryPort sesiones,
                                    ClienteStripeRepositoryPort clientes,
                                    SincronizadorSuscripciones sincronizador,
                                    Clock clock) {
        this.proveedor = proveedor;
        this.sesiones = sesiones;
        this.clientes = clientes;
        this.sincronizador = sincronizador;
        this.clock = clock;
    }

    @Override
    public boolean soporta(String tipo) {
        return TIPOS.contains(tipo);
    }

    @Override
    public void procesar(EventoStripe evento) {
        SesionProveedor remota = proveedor.obtenerSesion(evento.getObjetoId());
        Optional<SesionCheckout> local = sesiones.findByStripeSessionId(remota.id());
        if (local.isEmpty()) {
            throw new SuscripcionAjenaException(
                    "La sesion " + remota.id() + " no existe en M10");
        }
        SesionCheckout sesion = local.get();
        Instant ahora = clock.instant();
        if (EXPIRADA.equals(evento.getTipo())) {
            sesion.marcarExpirada();
            sesiones.save(sesion);
            return;
        }
        sesion.marcarCompletada(remota.customerId(), ahora);
        sesiones.save(sesion);
        guardarCliente(sesion.getUserId(), remota.customerId(), ahora);
        if (remota.subscriptionId() == null || remota.subscriptionId().isBlank()) {
            log.warn("La sesion {} se completo sin suscripcion asociada", remota.id());
            return;
        }
        sincronizador.sincronizar(remota.subscriptionId());
    }

    private void guardarCliente(Long userId, String customerId, Instant ahora) {
        if (customerId == null || customerId.isBlank() || clientes.findById(userId).isPresent()) {
            return;
        }
        clientes.save(ClienteStripe.crear(userId, customerId, ahora));
    }
}
