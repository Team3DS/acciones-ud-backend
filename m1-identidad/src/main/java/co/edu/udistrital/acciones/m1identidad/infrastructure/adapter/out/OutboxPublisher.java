package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    private final RabbitTemplate rabbitTemplate;
    public OutboxPublisher(RabbitTemplate rabbitTemplate) { this.rabbitTemplate = rabbitTemplate; }

    public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
        rabbitTemplate.convertAndSend("m1.outbox", null, java.util.Map.of(
            "aggregateType", aggregateType,
            "aggregateId", aggregateId,
            "eventType", eventType,
            "payload", payload
        ));
    }
}
