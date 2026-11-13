package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.PublicadorCatalogoPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class CatalogoOutboxRelay {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogoOutboxRelay.class);
    private final JdbcTemplate jdbc;
    private final PublicadorCatalogoPort publisher;
    private final TransactionTemplate transaction;

    public CatalogoOutboxRelay(
            JdbcTemplate jdbc,
            PublicadorCatalogoPort publisher,
            PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.publisher = publisher;
        transaction = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelayString = "${app.outbox.interval-ms:2000}")
    public void publicarPendientes() {
        transaction.executeWithoutResult(
                status -> {
                    var events =
                            jdbc.query(
                                    "SELECT id,routing_key,body::text AS"
                                        + " body,body->>'correlationId' AS correlation FROM"
                                        + " outbox_evento WHERE publicado_en IS NULL AND"
                                        + " proximo_intento<=CURRENT_TIMESTAMP ORDER BY creado_en"
                                        + " FOR UPDATE SKIP LOCKED LIMIT 10",
                                    (rs, row) ->
                                            new Pending(
                                                    rs.getObject("id", UUID.class),
                                                    rs.getString("routing_key"),
                                                    rs.getString("body"),
                                                    rs.getString("correlation")));
                    for (Pending event : events) {
                        if (event.correlation() != null)
                            MDC.put("correlationId", event.correlation());
                        try {
                            publisher.publicar(event.id(), event.routingKey(), event.json());
                            jdbc.update(
                                    "UPDATE outbox_evento SET"
                                            + " publicado_en=CURRENT_TIMESTAMP,intentos=intentos+1"
                                            + " WHERE id=?",
                                    event.id());
                        } catch (RuntimeException exception) {
                            jdbc.update(
                                    "UPDATE outbox_evento SET"
                                        + " intentos=intentos+1,proximo_intento=CURRENT_TIMESTAMP+INTERVAL"
                                        + " '30 seconds' WHERE id=?",
                                    event.id());
                            LOG.warn(
                                    "outbox_retry eventId={} type={}",
                                    event.id(),
                                    exception.getClass().getSimpleName());
                        } finally {
                            MDC.remove("correlationId");
                        }
                    }
                });
    }

    private record Pending(UUID id, String routingKey, String json, String correlation) {}
}
