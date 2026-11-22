package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ProcesarEventosStripeUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.webhooks", name = "procesador-enabled",
        havingValue = "true", matchIfMissing = true)
public class ProcesadorEventosStripe {

    private static final Logger log = LoggerFactory.getLogger(ProcesadorEventosStripe.class);

    private final ProcesarEventosStripeUseCase procesarEventos;

    public ProcesadorEventosStripe(ProcesarEventosStripeUseCase procesarEventos) {
        this.procesarEventos = procesarEventos;
    }

    @Scheduled(fixedDelayString = "${app.webhooks.fixed-delay:PT10S}")
    public void procesar() {
        try {
            procesarEventos.procesarPendientes();
        } catch (RuntimeException ex) {
            log.error("No se pudieron procesar los eventos pendientes de Stripe", ex);
        }
    }
}
