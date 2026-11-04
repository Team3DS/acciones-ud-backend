package co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out;

import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {
    public void publish(String aggregateType, String aggregateId, String eventType, String payload) {
        // Stub: en producción publicar a RabbitMQ
    }
}
