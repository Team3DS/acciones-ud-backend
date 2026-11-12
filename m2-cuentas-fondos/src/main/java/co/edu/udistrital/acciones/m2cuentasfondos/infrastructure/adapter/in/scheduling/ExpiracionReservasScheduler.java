package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ExpirarReservasUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.reservas.expiracion", name = "habilitada", havingValue = "true",
        matchIfMissing = true)
public class ExpiracionReservasScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionReservasScheduler.class);

    private final ExpirarReservasUseCase expirarReservas;

    public ExpiracionReservasScheduler(ExpirarReservasUseCase expirarReservas) {
        this.expirarReservas = expirarReservas;
    }

    @Scheduled(fixedDelayString = "${app.reservas.expiracion.intervalo-ms:60000}")
    public void expirarPendientes() {
        try {
            int expiradas = expirarReservas.ejecutar();
            if (expiradas > 0) {
                log.info("El scheduler expiró {} reservas", expiradas);
            }
        } catch (RuntimeException ex) {
            log.warn("Falló la expiración programada de reservas: {}", ex.getMessage(), ex);
        }
    }
}
