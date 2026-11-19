package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.EventosService;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.config.RabbitConfig;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class NotificacionConsumer {
    private final EventosService service;
    private final EventoDecoder decoder;
    private final RabbitTemplate rabbit;

    public NotificacionConsumer(
            EventosService service, EventoDecoder decoder, RabbitTemplate rabbit) {
        this.service = service;
        this.decoder = decoder;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE, ackMode = "MANUAL")
    public void consumir(Message message, Channel channel) throws IOException {
        message.getMessageProperties()
                .setContentType(org.springframework.amqp.core.MessageProperties.CONTENT_TYPE_BYTES);
        long tag = message.getMessageProperties().getDeliveryTag();
        String original = message.getMessageProperties().getHeader("m8-original-key");
        String key =
                original == null
                        ? message.getMessageProperties().getReceivedRoutingKey()
                        : original;
        try {
            service.procesar(
                    decoder.decode(
                            message.getBody(), key, message.getMessageProperties().getMessageId()));
            channel.basicAck(tag, false);
        } catch (AvisoException exception) {
            if (exception.status() < 500) {
                channel.basicReject(tag, false);
            } else {
                reintentar(message, channel, tag, key);
            }
        } catch (DataAccessException
                | org.springframework.transaction.TransactionException exception) {
            reintentar(message, channel, tag, key);
        }
    }

    private void reintentar(Message message, Channel channel, long tag, String key)
            throws IOException {
        Object header = message.getMessageProperties().getHeaders().get("m8-attempt");
        int attempts = header instanceof Number number ? number.intValue() : 0;
        if (attempts >= 3) {
            channel.basicReject(tag, false);
            return;
        }
        try {
            Message retry =
                    MessageBuilder.fromClonedMessage(message)
                            .setHeader("m8-attempt", attempts + 1)
                            .setHeader("m8-original-key", key)
                            .setDeliveryMode(
                                    org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT)
                            .build();
            CorrelationData confirmation = new CorrelationData(UUID.randomUUID().toString());
            rabbit.send("acciones.ud.retry", "m8.retry", retry, confirmation);
            var result = confirmation.getFuture().get(5, TimeUnit.SECONDS);
            if (!result.isAck() || confirmation.getReturned() != null) {
                channel.basicNack(tag, false, true);
                return;
            }
            channel.basicAck(tag, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            channel.basicNack(tag, false, true);
        } catch (org.springframework.amqp.AmqpException
                | java.util.concurrent.ExecutionException
                | java.util.concurrent.TimeoutException exception) {
            channel.basicNack(tag, false, true);
        }
    }
}
