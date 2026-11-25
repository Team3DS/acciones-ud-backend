package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.application.port.out.MfaNotificationPort;
import co.edu.udistrital.acciones.m1identidad.application.port.out.PasswordResetNotificationPort;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Publica {@code m1.mfa.solicitado} en el exchange {@code acciones.ud} con el sobre que valida M8
 * ({@code EventoDecoder}): eventId, eventType, source, occurredAt y payload con userId, email, code y expiresAt.
 */
@Component
public class RabbitMfaNotificationAdapter implements MfaNotificationPort, PasswordResetNotificationPort {

    public static final String EVENT_TYPE = "m1.mfa.solicitado";
    private static final String SOURCE = "M1";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public RabbitMfaNotificationAdapter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this(rabbitTemplate, objectMapper, Clock.systemUTC());
    }

    RabbitMfaNotificationAdapter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, Clock clock) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void codigoSolicitado(Long userId, String correo, String codigo, Instant expiraEn) {
        publish(EVENT_TYPE, userId, correo, codigo, expiraEn);
    }

    @Override
    public void recuperacionSolicitada(Long userId, String correo, String codigo, Instant expiraEn) {
        publish("m1.recuperacion.solicitada", userId, correo, codigo, expiraEn);
    }

    private void publish(String eventType, Long userId, String correo, String codigo, Instant expiraEn) {
        String eventId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId);
        payload.put("email", correo);
        payload.put("code", codigo);
        payload.put("expiresAt", expiraEn.toString());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", eventType);
        envelope.put("source", SOURCE);
        envelope.put("occurredAt", Instant.now(clock).toString());
        envelope.put("payload", payload);

        Message message = MessageBuilder.withBody(toJson(envelope))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding("UTF-8")
                .setMessageId(eventId)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
        rabbitTemplate.send(RabbitConfig.EVENTS_EXCHANGE, eventType, message);
    }

    private byte[] toJson(Map<String, Object> envelope) {
        try {
            return objectMapper.writeValueAsBytes(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento " + EVENT_TYPE, e);
        }
    }
}
