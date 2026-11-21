package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicadorOrdenesAdapterTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    private PublicadorOrdenesAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new PublicadorOrdenesAdapter(rabbitTemplate);
    }

    @Test
    void publicarOrdenCreada() {
        Long ordenId = 1L;
        Long usuarioId = 10L;
        String symbol = "AAPL";
        String side = "COMPRA";
        Integer quantity = 100;
        String type = "MARKET";

        adapter.publicarOrdenCreada(ordenId, usuarioId, symbol, side, quantity, type);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.creada"), payloadCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals(ordenId, payload.get("orderId"));
        assertEquals(usuarioId, payload.get("userId"));
        assertEquals(symbol, payload.get("symbol"));
        assertEquals(side, payload.get("side"));
        assertEquals(quantity, payload.get("qty"));
        assertEquals(type, payload.get("type"));
        assertNotNull(payload.get("eventId"));
    }

    @Test
    void publicarOrdenEjecutada() {
        UUID eventId = UUID.randomUUID();
        Long executionId = 500L;
        Long ordenId = 1L;
        Long userId = 10L;
        String symbol = "TSLA";
        String side = "VENTA";
        Integer quantity = 50;
        BigDecimal price = new BigDecimal("250.75");
        BigDecimal commission = new BigDecimal("5.00");
        Instant executedAt = Instant.parse("2026-01-15T14:30:00Z");

        adapter.publicarOrdenEjecutada(eventId, executionId, ordenId, userId, symbol, side, quantity, price, commission, executedAt);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.ejecutada"), payloadCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals(eventId.toString(), payload.get("eventId"));
        assertEquals(executionId, payload.get("executionId"));
        assertEquals(ordenId, payload.get("orderId"));
        assertEquals(userId, payload.get("userId"));
        assertEquals(symbol, payload.get("symbol"));
        assertEquals(side, payload.get("side"));
        assertEquals(quantity, payload.get("quantity"));
        assertEquals(price, payload.get("price"));
        assertEquals(commission, payload.get("commission"));
        assertEquals(executedAt.toString(), payload.get("executedAt"));
    }

    @Test
    void publicarOrdenCancelada() {
        Long ordenId = 42L;
        String motivo = "Usuario solicitó cancelación";

        adapter.publicarOrdenCancelada(ordenId, motivo);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.cancelada"), payloadCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals(ordenId, payload.get("orderId"));
        assertEquals(motivo, payload.get("reason"));
        assertNotNull(payload.get("eventId"));
    }

    @Test
    void publicarOrdenRechazadaConBrokerError() {
        Long ordenId = 99L;
        String motivo = "Fondos insuficientes";
        String brokerError = "INSUFFICIENT_BUYING_POWER";

        adapter.publicarOrdenRechazada(ordenId, motivo, brokerError);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.rechazada"), payloadCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals(ordenId, payload.get("orderId"));
        assertEquals(motivo, payload.get("reason"));
        assertEquals(brokerError, payload.get("brokerError"));
        assertNotNull(payload.get("eventId"));
    }

    @Test
    void publicarOrdenRechazadaSinBrokerError() {
        Long ordenId = 99L;
        String motivo = "Fondos insuficientes";

        adapter.publicarOrdenRechazada(ordenId, motivo, null);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("acciones.ud"), eq("m5.orden.rechazada"), payloadCaptor.capture(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> payload = (java.util.Map<String, Object>) payloadCaptor.getValue();
        assertEquals(ordenId, payload.get("orderId"));
        assertEquals(motivo, payload.get("reason"));
        assertEquals("", payload.get("brokerError"));
    }

    @Test
    void enviarExcepcionNoPropaga() {
        doThrow(new RuntimeException("Error de conexión")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(), any(org.springframework.amqp.core.MessagePostProcessor.class));

        assertDoesNotThrow(() -> adapter.publicarOrdenCreada(1L, 1L, "AAPL", "COMPRA", 10, "MARKET"));
    }
}