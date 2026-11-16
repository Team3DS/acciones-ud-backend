package co.edu.udistrital.acciones.m8notificaciones.application.service;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.ServiciosPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class ReportesService {
    private static final Logger LOG = LoggerFactory.getLogger(ReportesService.class);
    private final AvisosRepositoryPort repository;
    private final ServiciosPort services;
    private final NotificacionesService notifications;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ReportesService(
            AvisosRepositoryPort repository,
            ServiciosPort services,
            NotificacionesService notifications,
            PlatformTransactionManager manager,
            Clock clock) {
        this.repository = repository;
        this.services = services;
        this.notifications = notifications;
        this.transaction = new TransactionTemplate(manager);
        this.clock = clock;
    }

    public void generarMesAnterior() {
        YearMonth month = YearMonth.now(clock).minusMonths(1);
        for (int page = 0; ; page++) {
            var subscribers = services.suscriptoresReporte(page);
            if (subscribers.isEmpty()) {
                return;
            }
            for (var subscriber : subscribers) {
                try {
                    generar(subscriber.userId(), subscriber.email(), month);
                } catch (AvisoException exception) {
                    LOG.warn(
                            "reporte_pendiente usuario={} periodo={} estado={}",
                            subscriber.userId(),
                            month,
                            exception.status());
                }
            }
        }
    }

    public void generar(long user, String email, YearMonth month) {
        if (user <= 0 || month == null || !month.isBefore(YearMonth.now(clock))) {
            throw new AvisoException(400, "Periodo inválido.");
        }
        var summary = services.resumen(user, month);
        if (summary.grossProfit() == null
                || summary.commissions() == null
                || summary.commissions().signum() < 0
                || !"USD".equals(summary.currency())
                || summary.assets() == null) {
            throw new AvisoException(503, "Reporte incompleto en los servicios de origen.");
        }
        String message =
                "Periodo "
                        + month
                        + ". Resultado bruto: "
                        + summary.grossProfit().toPlainString()
                        + " USD. Comisiones: "
                        + summary.commissions().toPlainString()
                        + " USD. Activos: "
                        + summary.assets();
        if (message.length() > 2000) {
            throw new AvisoException(503, "Resumen de activos excede el contrato.");
        }
        transaction.executeWithoutResult(
                status -> {
                    long report = repository.crearReporte(user, month);
                    if (report == 0) {
                        return;
                    }
                    UUID event =
                            UUID.nameUUIDFromBytes(
                                    ("m8.reporte:" + user + ":" + month)
                                            .getBytes(StandardCharsets.UTF_8));
                    notifications.crear(
                            event,
                            user,
                            TipoAviso.REPORTE_MENSUAL,
                            "Reporte mensual " + month,
                            message,
                            null,
                            null,
                            email,
                            report);
                });
    }
}
