package co.edu.udistrital.acciones.m8notificaciones.unit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import co.edu.udistrital.acciones.m8notificaciones.application.service.EventosService;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in.EventoDecoder;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in.NotificacionConsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.TransientDataAccessResourceException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

class NotificacionConsumerTest {
    private EventosService service;
    private RabbitTemplate rabbit;
    private Channel channel;
    private NotificacionConsumer consumer;
    private Message message;

    @BeforeEach
    void setup() {
        service = mock(EventosService.class);
        rabbit = mock(RabbitTemplate.class);
        channel = mock(Channel.class);
        consumer = new NotificacionConsumer(service, new EventoDecoder(new ObjectMapper()), rabbit);
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(12);
        properties.setReceivedRoutingKey("m5.orden.creada");
        String body =
                "{\"eventId\":\""
                        + UUID.randomUUID()
                        + "\",\"eventType\":\"m5.orden.creada\",\"occurredAt\":\"2026-11-16T10:00:00Z\",\"payload\":{\"userId\":1}}";
        message = new Message(body.getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void confirmaSoloDespuesDeProcesar() throws Exception {
        consumer.consumir(message, channel);
        var order = org.mockito.Mockito.inOrder(service, channel);
        order.verify(service).procesar(any());
        order.verify(channel).basicAck(12, false);
    }

    @Test
    void rechazaEventoInvalidoSinReencolar() throws Exception {
        doThrow(new AvisoException(400, "inválido")).when(service).procesar(any());
        consumer.consumir(message, channel);
        verify(channel).basicReject(12, false);
        verify(channel, never()).basicAck(12, false);
    }

    @Test
    void publicaReintentoPersistenteYEsperaConfirmacion() throws Exception {
        doThrow(new TransientDataAccessResourceException("fixture")).when(service).procesar(any());
        doAnswer(
                        invocation -> {
                            CorrelationData data = invocation.getArgument(3);
                            data.getFuture().complete(new CorrelationData.Confirm(true, null));
                            return null;
                        })
                .when(rabbit)
                .send(
                        eq("acciones.ud.retry"),
                        eq("m8.retry"),
                        any(Message.class),
                        any(CorrelationData.class));
        consumer.consumir(message, channel);
        verify(channel).basicAck(12, false);
        verify(rabbit)
                .send(
                        eq("acciones.ud.retry"),
                        eq("m8.retry"),
                        org.mockito.ArgumentMatchers.argThat(
                                m ->
                                        Integer.valueOf(1)
                                                .equals(
                                                        m.getMessageProperties()
                                                                .getHeader("m8-attempt"))),
                        any(CorrelationData.class));
    }

    @Test
    void noPierdeOriginalSiFallaPublicarReintento() throws Exception {
        doThrow(new TransientDataAccessResourceException("fixture")).when(service).procesar(any());
        doThrow(new org.springframework.amqp.AmqpException("fixture"))
                .when(rabbit)
                .send(
                        any(String.class),
                        any(String.class),
                        any(Message.class),
                        any(CorrelationData.class));
        consumer.consumir(message, channel);
        verify(channel).basicNack(12, false, true);
        verify(channel, never()).basicAck(12, false);
    }

    @Test
    void agotaReintentosHaciaDlq() throws Exception {
        message.getMessageProperties().setHeader("m8-attempt", 3);
        doThrow(new TransientDataAccessResourceException("fixture")).when(service).procesar(any());
        consumer.consumir(message, channel);
        verify(channel).basicReject(12, false);
    }
}
