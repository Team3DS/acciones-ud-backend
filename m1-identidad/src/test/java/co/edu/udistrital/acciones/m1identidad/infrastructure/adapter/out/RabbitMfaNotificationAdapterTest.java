package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m1identidad.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Comprueba que el evento cumple las validaciones de M8 (EventoDecoder y EventosService.autenticacion):
 * eventType igual a la routing key, source M1, eventId UUID, occurredAt ISO-8601 y payload con
 * userId, email, code de 6 dígitos y expiresAt a menos de 15 minutos.
 */
class RabbitMfaNotificationAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-21T22:00:00Z");

    private RabbitTemplate rabbitTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private RabbitMfaNotificationAdapter adapter;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        adapter = new RabbitMfaNotificationAdapter(rabbitTemplate, mapper, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void publishesMfaRequestedEventWithEnvelopeExpectedByM8() throws Exception {
        Instant expiresAt = NOW.plusSeconds(300);

        adapter.codigoSolicitado(42L, "ana@udistrital.edu.co", "048213", expiresAt);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitConfig.EVENTS_EXCHANGE), eq("m1.mfa.solicitado"), captor.capture());
        Message message = captor.getValue();
        JsonNode envelope = mapper.readTree(message.getBody());

        assertEquals("m1.mfa.solicitado", envelope.get("eventType").asText());
        assertEquals("M1", envelope.get("source").asText());
        String eventId = envelope.get("eventId").asText();
        assertDoesNotThrow(() -> UUID.fromString(eventId));
        assertEquals(eventId, message.getMessageProperties().getMessageId());
        assertEquals(NOW, Instant.parse(envelope.get("occurredAt").asText()));

        JsonNode payload = envelope.get("payload");
        assertEquals(42L, payload.get("userId").asLong());
        assertEquals("ana@udistrital.edu.co", payload.get("email").asText());
        assertEquals("048213", payload.get("code").asText());
        assertEquals(expiresAt, Instant.parse(payload.get("expiresAt").asText()));

        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
    }

    @Test
    void keepsLeadingZerosOfTheCodeAsText() throws Exception {
        adapter.codigoSolicitado(7L, "a@b.co", "000123", NOW.plusSeconds(60));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitConfig.EVENTS_EXCHANGE), eq("m1.mfa.solicitado"), captor.capture());
        JsonNode code = mapper.readTree(captor.getValue().getBody()).get("payload").get("code");
        assertEquals(true, code.isTextual(), "M8 exige que code sea texto");
        assertEquals("000123", code.asText());
    }
}
