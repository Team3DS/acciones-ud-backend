package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.PublicadorCatalogoPort;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitCatalogoPublisher implements PublicadorCatalogoPort {
    private final RabbitTemplate rabbit;

    public RabbitCatalogoPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publicar(UUID id, String routingKey, String json) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        properties.setMessageId(id.toString());
        String correlation = org.slf4j.MDC.get("correlationId");
        if (correlation != null) properties.setHeader("X-Correlation-ID", correlation);
        CorrelationData correlationData = new CorrelationData(id.toString());
        rabbit.send(
                "acciones.ud",
                routingKey,
                new Message(json.getBytes(StandardCharsets.UTF_8), properties),
                correlationData);
        try {
            var confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.isAck() || correlationData.getReturned() != null)
                throw new IllegalStateException("El broker no confirmó la entrega a una cola.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Publicación interrumpida.", exception);
        } catch (java.util.concurrent.ExecutionException
                | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("No se recibió confirmación del broker.", exception);
        }
    }
}
