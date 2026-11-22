package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.service.EventosOrden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publica los eventos de órdenes en {@code acciones.ud} con el sobre estándar de {@link EventosOrden}.
 * El {@code eventId} lo da el caso de uso para que coincida con el del outbox.
 */
@Component
public class PublicadorOrdenesAdapter implements PublicadorOrdenesPort {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOrdenesAdapter.class);

    public static final String EXCHANGE = "acciones.ud";
    public static final String RK_ORDEN_CREADA = EventosOrden.ORDEN_CREADA;
    public static final String RK_ORDEN_EJECUTADA = EventosOrden.ORDEN_EJECUTADA;
    public static final String RK_ORDEN_CANCELADA = EventosOrden.ORDEN_CANCELADA;
    public static final String RK_ORDEN_RECHAZADA = EventosOrden.ORDEN_RECHAZADA;

    private final RabbitTemplate rabbitTemplate;

    public PublicadorOrdenesAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarOrdenCreada(UUID eventId, Long ordenId, Long usuarioId, String symbol,
                                    String side, Integer quantity, String type) {
        enviar(eventId, RK_ORDEN_CREADA, ordenId,
                EventosOrden.creada(ordenId, usuarioId, symbol, side, quantity, type));
    }

    @Override
    public void publicarOrdenEjecutada(UUID eventId, Long executionId, Long ordenId, Long userId,
                                       String symbol, String side, Integer quantity,
                                       BigDecimal price, BigDecimal commission, Instant executedAt) {
        enviar(eventId, RK_ORDEN_EJECUTADA, ordenId,
                EventosOrden.ejecutada(executionId, ordenId, userId, symbol, side, quantity, price, commission, executedAt));
    }

    @Override
    public void publicarOrdenCancelada(UUID eventId, Long ordenId, Long usuarioId, String motivo) {
        enviar(eventId, RK_ORDEN_CANCELADA, ordenId, EventosOrden.cancelada(ordenId, usuarioId, motivo));
    }

    @Override
    public void publicarOrdenRechazada(UUID eventId, Long ordenId, Long usuarioId, String motivo, String brokerError) {
        enviar(eventId, RK_ORDEN_RECHAZADA, ordenId, EventosOrden.rechazada(ordenId, usuarioId, motivo, brokerError));
    }

    private void enviar(UUID eventId, String routingKey, Long ordenId, Map<String, Object> payload) {
        Map<String, Object> sobre = EventosOrden.sobre(eventId, routingKey, ordenId, payload);
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, sobre, m -> {
                m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                m.getMessageProperties().setMessageId(eventId.toString());
                return m;
            });
        } catch (Exception ex) {
            log.error("Error publicando evento en {}: {}", routingKey, ex.getMessage(), ex);
        }
    }
}
