package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventosOrdenTest {

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void traduceElLadoDelDominioAlDelContrato() {
        assertEquals("BUY", EventosOrden.lado("COMPRA"));
        assertEquals("SELL", EventosOrden.lado("venta"));
        assertEquals("BUY", EventosOrden.lado("BUY"));
        assertEquals("SELL", EventosOrden.lado(" sell "));
        assertEquals("OTRO", EventosOrden.lado("OTRO"));
        assertNull(EventosOrden.lado(null));
    }

    @Test
    void calculaElTotalSegunElLado() {
        assertEquals(new BigDecimal("1002.50"), EventosOrden.totalAmount("COMPRA", 10, new BigDecimal("100.00"), new BigDecimal("2.50")));
        assertEquals(new BigDecimal("997.50"), EventosOrden.totalAmount("VENTA", 10, new BigDecimal("100.00"), new BigDecimal("2.50")));
        assertEquals(new BigDecimal("1000.00"), EventosOrden.totalAmount("BUY", 10, new BigDecimal("100.00"), null));
        assertEquals(new BigDecimal("1.23"), EventosOrden.totalAmount("BUY", 1, new BigDecimal("1.2345"), null));
        assertNull(EventosOrden.totalAmount("BUY", null, new BigDecimal("1"), null));
        assertNull(EventosOrden.totalAmount("BUY", 1, null, null));
    }

    @Test
    void elSobreUsaElCorrelationIdDeLaSolicitudSiExiste() {
        UUID eventId = UUID.randomUUID();
        MDC.put("correlationId", "req-123");

        Map<String, Object> sobre = EventosOrden.sobre(eventId, EventosOrden.ORDEN_CANCELADA, 9L, EventosOrden.cancelada(9L, 1L, "x"));

        assertEquals("req-123", sobre.get("correlationId"));
        assertEquals("9", sobre.get("aggregateId"));
        assertEquals(EventosOrden.ORDEN_CANCELADA, sobre.get("eventType"));
    }

    @Test
    void sinSolicitudElCorrelationIdEsElEventId() {
        UUID eventId = UUID.randomUUID();

        Map<String, Object> sobre = EventosOrden.sobre(eventId, EventosOrden.ORDEN_CREADA, 1L, EventosOrden.creada(1L, 2L, "AAPL", "COMPRA", 1, "MARKET"));

        assertEquals(eventId.toString(), sobre.get("correlationId"));
    }

    @Test
    void laEjecucionSinComisionNiFechaUsaValoresPorDefecto() {
        Map<String, Object> payload = EventosOrden.ejecutada("exec-1", 2L, 3L, "AAPL", "COMPRA", 1, new BigDecimal("10.00"), null, null);

        assertEquals(BigDecimal.ZERO, payload.get("commission"));
        assertNotNull(payload.get("executedAt"));
        assertEquals("exec-1", payload.get("executionId"));
        assertEquals("2", payload.get("orderId"));
    }
}
