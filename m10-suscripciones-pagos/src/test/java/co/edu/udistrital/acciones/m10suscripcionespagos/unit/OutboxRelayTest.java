package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EventoSaliente;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.scheduling.OutboxRelay;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant CREADO = Instant.parse("2026-03-04T10:00:00Z");
    private static final Instant PUBLICADO = Instant.parse("2026-03-04T10:00:05Z");
    private static final Clock CLOCK = Clock.fixed(PUBLICADO, ZoneOffset.UTC);
    private static final String PAYLOAD = """
            {"userId":4101,"planId":"PREMIUM","interval":"MENSUAL","stripeSubscriptionId":"sub_abc",
             "validUntil":"2026-04-01T00:00:00Z"}
            """;

    @Mock
    private OutboxEventoRepositoryPort repositorio;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();
    private final AtomicReference<Message> entregado = new AtomicReference<>();
    private OutboxRelay relay;

    @BeforeEach
    void prepararRelay() {
        relay = new OutboxRelay(repositorio, rabbitTemplate, json, CLOCK, 25);
    }

    @Test
    void publicaElPendienteConElSobreYLasCabeceras() {
        OutboxEvento evento = evento(PAYLOAD);
        when(repositorio.buscarPendientes(25)).thenReturn(List.of(evento));
        doAnswer(invocacion -> {
            MessagePostProcessor procesador = invocacion.getArgument(3);
            entregado.set(procesador.postProcessMessage(new Message(new byte[0], new MessageProperties())));
            return null;
        }).when(rabbitTemplate).convertAndSend(eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.SUSCRIPCION_ACTIVADA),
                any(EventoSaliente.class), any(MessagePostProcessor.class));

        relay.publicarPendientes();

        ArgumentCaptor<EventoSaliente> captor = ArgumentCaptor.forClass(EventoSaliente.class);
        verify(rabbitTemplate).convertAndSend(eq(RoutingKeys.EXCHANGE), eq(RoutingKeys.SUSCRIPCION_ACTIVADA),
                captor.capture(), any(MessagePostProcessor.class));
        EventoSaliente sobre = captor.getValue();
        assertThat(sobre.eventId()).isEqualTo(evento.getId());
        assertThat(sobre.eventType()).isEqualTo(RoutingKeys.SUSCRIPCION_ACTIVADA);
        assertThat(sobre.occurredAt()).isEqualTo(CREADO);
        assertThat(sobre.source()).isEqualTo("M10");
        assertThat(sobre.aggregateId()).isEqualTo("7");
        assertThat(sobre.correlationId()).isEqualTo("corr-1");
        assertThat(sobre.payload().toString()).contains("\"planId\":\"PREMIUM\"");

        MessageProperties propiedades = entregado.get().getMessageProperties();
        assertThat(propiedades.getMessageId()).isEqualTo(evento.getId().toString());
        assertThat(propiedades.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        assertThat((String) propiedades.getHeader(OutboxRelay.CABECERA_CORRELACION)).isEqualTo("corr-1");
        assertThat(propiedades.getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(evento.isPublicado()).isTrue();
        assertThat(evento.getProcesadoEn()).isEqualTo(PUBLICADO);
        assertThat(evento.getUltimoError()).isNull();
        verify(repositorio).guardar(evento);
    }

    @Test
    void omiteLaCabeceraDeCorrelacionCuandoElEventoNoLaTiene() {
        OutboxEvento evento = OutboxEvento.pendiente(UUID.randomUUID(),
                RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "7", RoutingKeys.SUSCRIPCION_ACTIVADA,
                PAYLOAD, null, CREADO);
        when(repositorio.buscarPendientes(25)).thenReturn(List.of(evento));
        doAnswer(invocacion -> {
            MessagePostProcessor procesador = invocacion.getArgument(3);
            entregado.set(procesador.postProcessMessage(new Message(new byte[0], new MessageProperties())));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(EventoSaliente.class),
                any(MessagePostProcessor.class));

        relay.publicarPendientes();

        assertThat((String) entregado.get().getMessageProperties()
                .getHeader(OutboxRelay.CABECERA_CORRELACION)).isNull();
        assertThat(evento.isPublicado()).isTrue();
    }

    @Test
    void registraElFalloCuandoRabbitRechazaElMensaje() {
        OutboxEvento evento = evento(PAYLOAD);
        when(repositorio.buscarPendientes(25)).thenReturn(List.of(evento));
        doThrow(new AmqpException("broker caido")).when(rabbitTemplate).convertAndSend(anyString(), anyString(),
                any(EventoSaliente.class), any(MessagePostProcessor.class));

        relay.publicarPendientes();

        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getProcesadoEn()).isNull();
        assertThat(evento.getReintentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).contains("broker caido");
        verify(repositorio).guardar(evento);
    }

    @Test
    void registraElFalloCuandoElPayloadNoEsJson() {
        OutboxEvento evento = evento("no-es-json");
        when(repositorio.buscarPendientes(25)).thenReturn(List.of(evento));

        relay.publicarPendientes();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(EventoSaliente.class),
                any(MessagePostProcessor.class));
        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isEqualTo(1);
        assertThat(evento.getUltimoError()).isNotBlank();
        verify(repositorio).guardar(evento);
    }

    @Test
    void noPublicaNadaCuandoNoHayPendientes() {
        when(repositorio.buscarPendientes(25)).thenReturn(List.of());

        relay.publicarPendientes();

        verifyNoInteractions(rabbitTemplate);
        verify(repositorio, never()).guardar(any(OutboxEvento.class));
    }

    private static OutboxEvento evento(String payload) {
        return OutboxEvento.pendiente(UUID.fromString("11111111-2222-3333-4444-555555555555"),
                RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "7", RoutingKeys.SUSCRIPCION_ACTIVADA,
                payload, "corr-1", CREADO);
    }
}
