package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PublicadorOrdenesAdapter implements PublicadorOrdenesPort {

    private static final Logger log = LoggerFactory.getLogger(PublicadorOrdenesAdapter.class);

    public static final String EXCHANGE = "acciones.ud";
    public static final String RK_ORDEN_CREADA = "m5.orden.creada";
    public static final String RK_ORDEN_EJECUTADA = "m5.orden.ejecutada";
    public static final String RK_ORDEN_CANCELADA = "m5.orden.cancelada";
    public static final String RK_ORDEN_RECHAZADA = "m5.orden.rechazada";

    private final RabbitTemplate rabbitTemplate;

    public PublicadorOrdenesAdapter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publicarOrdenCreada(Long ordenId, Long usuarioId, String symbol,
                                    String side, Integer quantity, String type) {
        Map<String, Object> payload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", ordenId,
                "userId", usuarioId,
                "symbol", symbol,
                "side", side,
                "qty", quantity,
                "type", type
        );
        enviar(RK_ORDEN_CREADA, payload);
    }

    @Override
    public void publicarOrdenEjecutada(UUID eventId, Long executionId, Long ordenId, Long userId,
                                       String symbol, String side, Integer quantity,
                                       BigDecimal price, BigDecimal commission, Instant executedAt) {
        Map<String, Object> payload = Map.of(
                "eventId", eventId.toString(),
                "executionId", executionId,
                "orderId", ordenId,
                "userId", userId,
                "symbol", symbol,
                "side", side,
                "quantity", quantity,
                "price", price,
                "commission", commission,
                "executedAt", executedAt != null ? executedAt.toString() : Instant.now().toString()
        );
        enviar(RK_ORDEN_EJECUTADA, payload);
    }

    @Override
    public void publicarOrdenCancelada(Long ordenId, String motivo) {
        Map<String, Object> payload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", ordenId,
                "reason", motivo
        );
        enviar(RK_ORDEN_CANCELADA, payload);
    }

    @Override
    public void publicarOrdenRechazada(Long ordenId, String motivo, String brokerError) {
        Map<String, Object> payload = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "orderId", ordenId,
                "reason", motivo,
                "brokerError", brokerError != null ? brokerError : ""
        );
        enviar(RK_ORDEN_RECHAZADA, payload);
    }

    private void enviar(String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, payload, m -> {
                m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return m;
            });
        } catch (Exception ex) {
            log.error("Error publicando evento en {}: {}", routingKey, ex.getMessage(), ex);
        }
    }
}
