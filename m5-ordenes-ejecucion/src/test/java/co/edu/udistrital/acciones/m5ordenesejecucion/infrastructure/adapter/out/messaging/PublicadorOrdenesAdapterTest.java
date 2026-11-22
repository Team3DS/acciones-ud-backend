package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * El publicador debe enviar el sobre que exigen los consumidores: M2 y M6 despachan por eventType = routing key
 * y leen el payload; M8 además valida source = "M5", eventId UUID, occurredAt y userId en el payload.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorOrdenesAdapterTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    private PublicadorOrdenesAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PublicadorOrdenesAdapter(rabbitTemplate);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sobreEnviado(String routingKey) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq(routingKey), captor.capture(), any(MessagePostProcessor.class));
        return (Map<String, Object>) captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> payload(Map<String, Object> sobre) {
        return (Map<String, Object>) sobre.get("payload");
    }

    private static void verificarSobre(Map<String, Object> sobre, UUID eventId, String tipo, String ordenId) {
        assertEquals(eventId.toString(), sobre.get("eventId"));
        assertEquals(tipo, sobre.get("eventType"));
        assertEquals("M5", sobre.get("source"));
        assertEquals(ordenId, sobre.get("aggregateId"));
        assertNotNull(sobre.get("correlationId"));
        assertDoesNotThrow(() -> Instant.parse((String) sobre.get("occurredAt")));
    }

    @Test
    void publicarOrdenCreada() {
        UUID eventId = UUID.randomUUID();

        adapter.publicarOrdenCreada(eventId, 1L, 10L, "AAPL", "COMPRA", 100, "MARKET");

        Map<String, Object> sobre = sobreEnviado("m5.orden.creada");
        verificarSobre(sobre, eventId, "m5.orden.creada", "1");
        Map<String, Object> payload = payload(sobre);
        assertEquals("1", payload.get("orderId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("AAPL", payload.get("symbol"));
        assertEquals("BUY", payload.get("side"));
        assertEquals(100, payload.get("qty"));
        assertEquals("MARKET", payload.get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicarOrdenEjecutadaConFillsYTotal() {
        UUID eventId = UUID.randomUUID();
        Instant executedAt = Instant.parse("2026-11-22T15:30:00Z");

        adapter.publicarOrdenEjecutada(eventId, "exec-500", 1L, 10L, "MSFT", "COMPRA", 50,
                new BigDecimal("200.50"), new BigDecimal("2.00"), executedAt);

        Map<String, Object> sobre = sobreEnviado("m5.orden.ejecutada");
        verificarSobre(sobre, eventId, "m5.orden.ejecutada", "1");
        Map<String, Object> payload = payload(sobre);
        assertEquals("1", payload.get("orderId"));
        assertEquals("exec-500", payload.get("executionId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("BUY", payload.get("side"));
        assertEquals(0, new BigDecimal("10027.00").compareTo((BigDecimal) payload.get("totalAmount")));
        List<Map<String, Object>> fills = (List<Map<String, Object>>) payload.get("fills");
        assertEquals(1, fills.size());
        assertEquals(new BigDecimal("200.50"), fills.get(0).get("price"));
        assertEquals(50, fills.get(0).get("qty"));
        assertEquals(executedAt.toString(), fills.get(0).get("timestamp"));
    }

    @Test
    void publicarOrdenEjecutadaDeVentaDescuentaLaComision() {
        adapter.publicarOrdenEjecutada(UUID.randomUUID(), "exec-501", 2L, 10L, "TSLA", "VENTA", 10,
                new BigDecimal("100.00"), new BigDecimal("20.00"), null);

        Map<String, Object> payload = payload(sobreEnviado("m5.orden.ejecutada"));
        assertEquals("SELL", payload.get("side"));
        assertEquals(0, new BigDecimal("980.00").compareTo((BigDecimal) payload.get("totalAmount")));
    }

    @Test
    void publicarOrdenCanceladaIncluyeUsuario() {
        UUID eventId = UUID.randomUUID();

        adapter.publicarOrdenCancelada(eventId, 3L, 10L, "Cancelada por el usuario");

        Map<String, Object> sobre = sobreEnviado("m5.orden.cancelada");
        verificarSobre(sobre, eventId, "m5.orden.cancelada", "3");
        Map<String, Object> payload = payload(sobre);
        assertEquals("3", payload.get("orderId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("Cancelada por el usuario", payload.get("reason"));
    }

    @Test
    void publicarOrdenRechazada() {
        UUID eventId = UUID.randomUUID();

        adapter.publicarOrdenRechazada(eventId, 4L, 10L, "Sin fondos", "insufficient buying power");

        Map<String, Object> payload = payload(sobreEnviado("m5.orden.rechazada"));
        assertEquals("4", payload.get("orderId"));
        assertEquals(10L, payload.get("userId"));
        assertEquals("Sin fondos", payload.get("reason"));
        assertEquals("insufficient buying power", payload.get("brokerError"));
    }

    @Test
    void publicarOrdenRechazadaSinErrorDelBroker() {
        adapter.publicarOrdenRechazada(UUID.randomUUID(), 4L, 10L, "Sin fondos", null);

        assertEquals("", payload(sobreEnviado("m5.orden.rechazada")).get("brokerError"));
    }

    @Test
    void marcaElMensajeComoJsonConElEventIdComoMessageId() throws Exception {
        UUID eventId = UUID.randomUUID();
        adapter.publicarOrdenCancelada(eventId, 3L, 10L, "motivo");

        ArgumentCaptor<MessagePostProcessor> captor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.cancelada"), any(Object.class), captor.capture());
        Message procesado = captor.getValue().postProcessMessage(new Message(new byte[0], new MessageProperties()));
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, procesado.getMessageProperties().getContentType());
        assertEquals(eventId.toString(), procesado.getMessageProperties().getMessageId());
    }

    @Test
    void enviarExcepcionNoPropaga() {
        doThrow(new RuntimeException("Error de conexión")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class));

        assertDoesNotThrow(() -> adapter.publicarOrdenCreada(UUID.randomUUID(), 1L, 1L, "AAPL", "COMPRA", 10, "MARKET"));
    }
}
