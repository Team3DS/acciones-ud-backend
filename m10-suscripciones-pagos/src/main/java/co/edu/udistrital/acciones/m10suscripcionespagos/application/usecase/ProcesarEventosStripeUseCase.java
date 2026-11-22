package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.EventoStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ManejadorEventoStripePort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.WebhooksProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProcesarEventosStripeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcesarEventosStripeUseCase.class);
    private static final int EXPONENTE_MAXIMO = 20;

    private final TransactionTemplate transacciones;
    private final EventoStripeRepositoryPort eventos;
    private final List<ManejadorEventoStripePort> manejadores;
    private final WebhooksProperties propiedades;
    private final Clock clock;

    public ProcesarEventosStripeUseCase(PlatformTransactionManager gestorTransacciones,
                                        EventoStripeRepositoryPort eventos,
                                        List<ManejadorEventoStripePort> manejadores,
                                        WebhooksProperties propiedades,
                                        Clock clock) {
        this.transacciones = new TransactionTemplate(gestorTransacciones);
        this.eventos = eventos;
        this.manejadores = List.copyOf(manejadores);
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public void procesarPendientes() {
        Instant ahora = clock.instant();
        for (String id : eventos.findPendientes(ahora, propiedades.maxIntentos(), propiedades.lote())) {
            procesar(id);
        }
    }

    private void procesar(String id) {
        try {
            transacciones.executeWithoutResult(estado -> procesarEnTransaccion(id));
        } catch (RuntimeException ex) {
            log.warn("Fallo el procesamiento del evento {}; se reintentara con backoff", id, ex);
            registrarFallo(id, ex);
        }
    }

    private void procesarEnTransaccion(String id) {
        Instant ahora = clock.instant();
        Optional<EventoStripe> pendiente = eventos.bloquearParaProcesar(id, ahora, propiedades.maxIntentos());
        if (pendiente.isEmpty()) {
            return;
        }
        EventoStripe evento = pendiente.get();
        despachar(evento);
        evento.marcarProcesado(Instant.now(clock));
        eventos.save(evento);
    }

    private void despachar(EventoStripe evento) {
        for (ManejadorEventoStripePort manejador : manejadores) {
            if (manejador.soporta(evento.getTipo())) {
                manejador.procesar(evento);
                return;
            }
        }
        log.warn("El evento {} de tipo '{}' no es de interes para M10; se marca como procesado",
                evento.getId(), evento.getTipo());
    }

    private void registrarFallo(String id, RuntimeException causa) {
        try {
            transacciones.executeWithoutResult(estado -> eventos.findById(id)
                    .filter(evento -> !evento.estaProcesado())
                    .ifPresent(evento -> {
                        Instant ahora = clock.instant();
                        evento.registrarFallo(detalle(causa), ahora, backoff(evento.getIntentos()));
                        eventos.save(evento);
                    }));
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar el fallo del evento {}", id, ex);
        }
    }

    private Duration backoff(int intentos) {
        int exponente = Math.min(Math.max(intentos, 0), EXPONENTE_MAXIMO);
        long factor = 1L << exponente;
        Duration calculado;
        try {
            calculado = propiedades.backoffBase().multipliedBy(factor);
        } catch (ArithmeticException ex) {
            calculado = propiedades.backoffMax();
        }
        return calculado.compareTo(propiedades.backoffMax()) > 0
                ? propiedades.backoffMax()
                : calculado;
    }

    private static String detalle(RuntimeException causa) {
        String mensaje = causa.getMessage();
        return mensaje == null || mensaje.isBlank() ? causa.getClass().getSimpleName() : mensaje;
    }
}
