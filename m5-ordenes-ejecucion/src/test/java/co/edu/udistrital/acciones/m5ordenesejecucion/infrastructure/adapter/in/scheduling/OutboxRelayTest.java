package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxEventoRepositoryPort outboxRepository;

    @Mock
    RabbitTemplate rabbitTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(outboxRepository, rabbitTemplate, 50);
    }

    @Test
    void procesarOutboxSinPendientes() {
        when(outboxRepository.findPendientes(50)).thenReturn(List.of());

        relay.procesarOutbox();

        verify(outboxRepository).findPendientes(50);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void procesarOutboxConEventos() {
        OutboxEvento evento = OutboxEvento.crear("Orden", "1", "OrdenCreada", "{\"orderId\":1}");
        when(outboxRepository.findPendientes(50)).thenReturn(List.of(evento));

        relay.procesarOutbox();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), eq("m5.orden.creada"), messageCaptor.capture());
        assertEquals("{\"orderId\":1}", new String(messageCaptor.getValue().getBody()));
        assertEquals(evento.getId().toString(), messageCaptor.getValue().getMessageProperties().getMessageId());

        assertNotNull(evento.getPublicadoEn());
        verify(outboxRepository).save(evento);
    }

    @Test
    void procesarOutboxFalloPublicacion() {
        OutboxEvento evento = OutboxEvento.crear("Orden", "1", "OrdenCreada", "{\"orderId\":1}");
        when(outboxRepository.findPendientes(50)).thenReturn(List.of(evento));
        doThrow(new RuntimeException("Error de conexión")).when(rabbitTemplate).send(anyString(), anyString(), any());

        relay.procesarOutbox();

        assertEquals(1, evento.getIntentos());
        assertEquals("Error de conexión", evento.getUltimoError());
        verify(outboxRepository).save(evento);
    }

    @Test
    void resolverRoutingKeyOrdenCreada() {
        String rk = invocarResolverRoutingKey("OrdenCreada");
        assertEquals("m5.orden.creada", rk);
    }

    @Test
    void resolverRoutingKeyOrdenEjecutada() {
        String rk = invocarResolverRoutingKey("OrdenEjecutada");
        assertEquals("m5.orden.ejecutada", rk);
    }

    @Test
    void resolverRoutingKeyOrdenCancelada() {
        String rk = invocarResolverRoutingKey("OrdenCancelada");
        assertEquals("m5.orden.cancelada", rk);
    }

    @Test
    void resolverRoutingKeyOrdenRechazada() {
        String rk = invocarResolverRoutingKey("OrdenRechazada");
        assertEquals("m5.orden.rechazada", rk);
    }

    @Test
    void resolverRoutingKeyDefault() {
        String rk = invocarResolverRoutingKey("TipoDesconocido");
        assertEquals("m5.orden.tipodesconocido", rk);
    }

    private String invocarResolverRoutingKey(String tipoEvento) {
        try {
            var method = OutboxRelay.class.getDeclaredMethod("resolverRoutingKey", String.class);
            method.setAccessible(true);
            return (String) method.invoke(relay, tipoEvento);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void procesarOutboxEventoTipoDesconocidoUsaDefaultCase() {
        OutboxEvento evento = OutboxEvento.crear("TipoDesconocido", "1", "TipoDesconocido", "{}");
        when(outboxRepository.findPendientes(50)).thenReturn(List.of(evento));

        relay.procesarOutbox();

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), eq("m5.orden.tipodesconocido"), messageCaptor.capture());
        assertTrue(evento.getPublicadoEn() != null);
        verify(outboxRepository).save(evento);
    }

    @Test
    void procesarOutboxMultiplesEventosUnoFallaContinuaConOtros() {
        OutboxEvento eventoOk = OutboxEvento.crear("Orden", "1", "OrdenCreada", "{\"orderId\":1}");
        OutboxEvento eventoFalla = OutboxEvento.crear("Orden", "2", "OrdenEjecutada", "{\"orderId\":2}");
        when(outboxRepository.findPendientes(50)).thenReturn(List.of(eventoOk, eventoFalla));
        doNothing().when(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), eq("m5.orden.creada"), any());
        doThrow(new RuntimeException("Error conexión")).when(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), eq("m5.orden.ejecutada"), any());

        relay.procesarOutbox();

        verify(rabbitTemplate, times(2)).send(anyString(), anyString(), any());
        assertTrue(eventoOk.getPublicadoEn() != null);
        assertEquals(1, eventoFalla.getIntentos());
        assertEquals("Error conexión", eventoFalla.getUltimoError());
        verify(outboxRepository).save(eventoOk);
        verify(outboxRepository).save(eventoFalla);
    }
}