package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m6portafolio.application.usecase.ValorarPosicionesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "app.valoracion", name = "scheduler-enabled", havingValue = "true",
        matchIfMissing = true)
public class ValoracionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ValoracionScheduler.class);

    private final ValorarPosicionesUseCase casoUso;

    public ValoracionScheduler(ValorarPosicionesUseCase casoUso) {
        this.casoUso = casoUso;
    }

    @Scheduled(fixedDelayString = "${app.valoracion.interval-seconds:30}", timeUnit = TimeUnit.SECONDS)
    public void valorar() {
        try {
            int valoradas = casoUso.ejecutar();
            if (valoradas > 0) {
                log.info("El scheduler valoro {} posiciones", valoradas);
            }
        } catch (RuntimeException ex) {
            log.warn("Fallo la valoracion programada: {}", ex.getMessage(), ex);
        }
    }
}
