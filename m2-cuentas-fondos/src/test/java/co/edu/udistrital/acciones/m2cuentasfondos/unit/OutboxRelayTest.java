package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.EventoDominio;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.scheduling.OutboxRelay;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA.plusSeconds(30), ZoneOffset.UTC);
    private static final UUID EVENTO_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private final OutboxEventoRepositoryPort outbox = mock(OutboxEventoRepositoryPort.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OutboxRelay relay = new OutboxRelay(outbox, rabbitTemplate, objectMapper, CLOCK, 50);

    @Test
    void publicaElEventoPendienteConSuSobreYCabeceras() {
        OutboxEvento evento = eventoPendiente();
        when(outbox.buscarPendientes(50)).thenReturn(List.of(evento));
        ArgumentCaptor<Object> cuerpo = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<MessagePostProcessor> postProcessor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        doAnswer(invocacion -> null).when(rabbitTemplate).convertAndSend(
                eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.MOVIMIENTO_CREADO),
                cuerpo.capture(), postProcessor.capture());

        relay.publicarPendientes();

        EventoDominio sobre = (EventoDominio) cuerpo.getValue();
        assertThat(sobre.eventId()).isEqualTo(EVENTO_ID);
        assertThat(sobre.eventType()).isEqualTo(RoutingKeys.MOVIMIENTO_CREADO);
        assertThat(sobre.occurredAt()).isEqualTo(AHORA);
        assertThat(sobre.aggregateId()).isEqualTo("55");
        assertThat(sobre.correlationId()).isEqualTo("trace-1");
        JsonNode payload = (JsonNode) sobre.payload();
        assertThat(payload.get("movementId").asLong()).isEqualTo(55L);

        Message mensaje = postProcessor.getValue().postProcessMessage(
                new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties()));
        assertThat(mensaje.getMessageProperties().getMessageId()).isEqualTo(EVENTO_ID.toString());
        assertThat(mensaje.getMessageProperties().getContentType()).isEqualTo("application/json");
        Object correlacionRecibida = mensaje.getMessageProperties().getHeader(OutboxRelay.CABECERA_CORRELACION);
        assertThat(correlacionRecibida).isEqualTo("trace-1");

        assertThat(evento.isPublicado()).isTrue();
        assertThat(evento.getProcesadoEn()).isEqualTo(AHORA.plusSeconds(30));
        verify(outbox).save(evento);
    }

    @Test
    void registraElFalloCuandoElBrokerNoResponde() {
        OutboxEvento evento = eventoPendiente();
        when(outbox.buscarPendientes(50)).thenReturn(List.of(evento));
        doThrow(new AmqpException("broker caído")).when(rabbitTemplate).convertAndSend(
                eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.MOVIMIENTO_CREADO),
                any(EventoDominio.class), any(MessagePostProcessor.class));

        relay.publicarPendientes();

        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).contains("broker caído");
        assertThat(evento.getProcesadoEn()).isNull();
        verify(outbox).save(evento);
    }

    @Test
    void noHaceNadaSiNoHayPendientes() {
        when(outbox.buscarPendientes(50)).thenReturn(List.of());

        relay.publicarPendientes();

        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class),
                any(Object.class), any(MessagePostProcessor.class));
        verify(outbox, never()).save(any());
    }

    private OutboxEvento eventoPendiente() {
        return OutboxEvento.crear(EVENTO_ID, "Movimiento", "55", RoutingKeys.MOVIMIENTO_CREADO,
                "{\"movementId\":55,\"userId\":5,\"type\":\"DEPOSITO\",\"amount\":20.00,\"balance\":80.00}",
                "trace-1", AHORA);
    }
}
