package co.edu.udistrital.acciones.m8notificaciones.application.service;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.application.port.out.ServiciosPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AlertaPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AlertasService {
    private static final Logger LOG = LoggerFactory.getLogger(AlertasService.class);
    private final AvisosRepositoryPort repository;
    private final ServiciosPort services;
    private final NotificacionesService notifications;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public AlertasService(
            AvisosRepositoryPort repository,
            ServiciosPort services,
            NotificacionesService notifications,
            Clock clock,
            PlatformTransactionManager manager) {
        this.repository = repository;
        this.services = services;
        this.notifications = notifications;
        this.clock = clock;
        this.transaction = new TransactionTemplate(manager);
    }

    public AlertaPrecio crear(
            long user, long instrument, CondicionPrecio condition, BigDecimal target, String key) {
        if (instrument <= 0
                || condition == null
                || target == null
                || target.signum() <= 0
                || target.scale() > 4
                || target.precision() - target.scale() > 14
                || key == null
                || !key.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw new AvisoException(400, "Datos o clave de idempotencia inválidos.");
        }
        if (!services.premium(user)) {
            throw new AvisoException(403, "Se requiere una suscripción premium activa.");
        }
        var asset = services.instrumento(instrument);
        if (!asset.enabled()) {
            throw new AvisoException(422, "El instrumento no está habilitado.");
        }
        String hash =
                hash(
                        instrument
                                + ":"
                                + condition
                                + ":"
                                + target.stripTrailingZeros().toPlainString());
        return transaction.execute(
                status ->
                        repository.crearAlerta(
                                user, instrument, asset.symbol(), condition, target, key, hash));
    }

    public List<AlertaPrecio> listar(long user) {
        return repository.alertas(user);
    }

    public void eliminar(long user, long id) {
        if (!repository.desactivarAlerta(user, id)) {
            throw new AvisoException(404, "Alerta no encontrada.");
        }
    }

    public void evaluar() {
        long after = 0;
        while (true) {
            List<AlertaPrecio> alerts = repository.alertasActivas(after);
            if (alerts.isEmpty()) {
                return;
            }
            for (AlertaPrecio alert : alerts) {
                try {
                    evaluar(alert);
                } catch (AvisoException exception) {
                    LOG.warn("alerta_pendiente id={} estado={}", alert.id(), exception.status());
                }
            }
            after = alerts.getLast().id();
        }
    }

    private void evaluar(AlertaPrecio alert) {
        if (!services.premium(alert.userId())) {
            repository.desactivarAlertas(alert.userId());
            return;
        }
        var instrument = services.instrumento(alert.instrumentId());
        if (!instrument.enabled()) {
            return;
        }
        var quote = services.cotizacion(instrument.symbol());
        if (quote.timestamp() == null
                || quote.timestamp().isBefore(clock.instant().minus(Duration.ofMinutes(2)))
                || quote.timestamp().isAfter(clock.instant().plusSeconds(30))
                || !alert.condition().cumple(quote.price(), alert.targetPrice())) {
            return;
        }
        transaction.executeWithoutResult(
                status -> {
                    if (repository.dispararAlerta(alert.id())) {
                        UUID event =
                                UUID.nameUUIDFromBytes(
                                        ("m8.alerta:" + alert.id())
                                                .getBytes(StandardCharsets.UTF_8));
                        notifications.crear(
                                event,
                                alert.userId(),
                                TipoAviso.ALERTA_PRECIO,
                                "Alerta de precio " + instrument.symbol(),
                                "El precio "
                                        + quote.price().toPlainString()
                                        + " de "
                                        + instrument.symbol()
                                        + " cumple "
                                        + alert.condition()
                                        + " "
                                        + alert.targetPrice().toPlainString()
                                        + ".",
                                null,
                                null,
                                null,
                                null);
                    }
                });
    }

    private static String hash(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible.");
        }
    }
}
