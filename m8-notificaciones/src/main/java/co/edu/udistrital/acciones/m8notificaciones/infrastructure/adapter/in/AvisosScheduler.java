package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.AlertasService;
import co.edu.udistrital.acciones.m8notificaciones.application.service.EntregasService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class AvisosScheduler {
    private final AlertasService alerts;
    private final EntregasService mail;

    public AvisosScheduler(AlertasService alerts, EntregasService mail) {
        this.alerts = alerts;
        this.mail = mail;
    }

    @Scheduled(fixedDelayString = "${app.jobs.mail-delay-ms:5000}")
    public void correos() {
        mail.procesar(50);
    }

    @Scheduled(fixedDelayString = "${app.jobs.alert-delay-ms:60000}")
    public void alertas() {
        alerts.evaluar();
    }
}
