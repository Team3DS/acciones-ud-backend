package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.infrastructure.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** At-least-once delivery with stable UUIDs; M8 deduplicates after a publish/commit interruption. */
@Component
@EnableScheduling
public class PasswordResetOutboxDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(PasswordResetOutboxDispatcher.class);
    private final JdbcTemplate sql;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transactions;
    private final boolean enabled;

    public PasswordResetOutboxDispatcher(JdbcTemplate sql, RabbitTemplate rabbit,
            PlatformTransactionManager transactionManager,
            @Value("${app.password-reset.outbox.enabled:true}") boolean enabled) {
        this.sql = sql;
        this.rabbit = rabbit;
        this.transactions = new TransactionTemplate(transactionManager);
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.password-reset.outbox.delay-ms:2000}")
    public void scheduledPublish() {
        if (!enabled) return;
        try { publishPending(); }
        catch (RuntimeException unavailable) { LOG.warn("password_reset_outbox_unavailable"); }
    }

    public int publishPending() {
        Integer delivered = transactions.execute(status -> {
            List<Pending> pending = sql.query("""
                    select id,carga::text from outbox_evento
                    where tipo_evento=? and publicado_en is null order by creado_en
                    limit 20 for update skip locked
                    """, (row, index) -> new Pending(row.getObject(1, UUID.class), row.getString(2)),
                    PasswordResetCompletionOutbox.EVENT_TYPE);
            int count = 0;
            for (Pending event : pending) {
                if (publishConfirmed(event)) {
                    sql.update("update outbox_evento set publicado_en=now(),intentos=coalesce(intentos,0)+1,ultimo_error=null where id=?", event.id());
                    count++;
                } else {
                    sql.update("update outbox_evento set intentos=coalesce(intentos,0)+1,ultimo_error='BROKER_UNCONFIRMED' where id=?", event.id());
                }
            }
            return count;
        });
        return delivered == null ? 0 : delivered;
    }

    private boolean publishConfirmed(Pending event) {
        CorrelationData correlation = new CorrelationData(event.id().toString());
        var message = MessageBuilder.withBody(event.envelope().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON).setContentEncoding("UTF-8")
                .setMessageId(event.id().toString()).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        try {
            rabbit.send(RabbitConfig.EVENTS_EXCHANGE, PasswordResetCompletionOutbox.EVENT_TYPE, message, correlation);
            var confirmation = correlation.getFuture().get(5, TimeUnit.SECONDS);
            return confirmation.isAck() && correlation.getReturned() == null;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception unavailable) {
            return false;
        }
    }

    private record Pending(UUID id, String envelope) {
        @Override
        public String toString() { return "Pending[redacted]"; }
    }
}
