package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out.OutboxPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new OutboxPublisher(rabbitTemplate);
    }

    @Test
    void publishSendsToExchangeWithCorrectPayload() {
        String aggregateType = "User";
        String aggregateId = "123";
        String eventType = "UserRegistered";
        String payload = "{\"correo\":\"test@test.com\"}";

        publisher.publish(aggregateType, aggregateId, eventType, payload);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate).convertAndSend(eq("m1.outbox"), eq(null), captor.capture());

        Map<String, String> sent = captor.getValue();
        assertEquals(aggregateType, sent.get("aggregateType"));
        assertEquals(aggregateId, sent.get("aggregateId"));
        assertEquals(eventType, sent.get("eventType"));
        assertEquals(payload, sent.get("payload"));
    }
}