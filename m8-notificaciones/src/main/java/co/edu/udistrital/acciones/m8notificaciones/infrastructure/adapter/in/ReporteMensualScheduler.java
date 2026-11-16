package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.ReportesService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.monthly.enabled", havingValue = "true")
public class ReporteMensualScheduler {
    private final ReportesService reports;

    public ReporteMensualScheduler(ReportesService reports) {
        this.reports = reports;
    }

    @Scheduled(cron = "${app.monthly.cron:0 0 7 1-7 * *}", zone = "America/Bogota")
    public void generar() {
        reports.generarMesAnterior();
    }
}
