package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.SincronizarEjecucionesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Revisa periódicamente las órdenes enviadas al broker y registra sus ejecuciones. */
@Component
@ConditionalOnProperty(name = "app.ejecuciones.habilitado", havingValue = "true", matchIfMissing = true)
public class SincronizadorEjecuciones {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorEjecuciones.class);

    private final SincronizarEjecucionesUseCase sincronizarEjecuciones;

    public SincronizadorEjecuciones(SincronizarEjecucionesUseCase sincronizarEjecuciones) {
        this.sincronizarEjecuciones = sincronizarEjecuciones;
    }

    @Scheduled(initialDelayString = "${app.ejecuciones.retraso-inicial-ms:15000}",
            fixedDelayString = "${app.ejecuciones.intervalo-ms:10000}")
    public void sincronizar() {
        try {
            int registradas = sincronizarEjecuciones.sincronizar();
            if (registradas > 0) {
                log.info("Ejecuciones registradas en esta pasada: {}", registradas);
            }
        } catch (Exception ex) {
            log.error("Fallo al sincronizar ejecuciones: {}", ex.getMessage(), ex);
        }
    }
}
