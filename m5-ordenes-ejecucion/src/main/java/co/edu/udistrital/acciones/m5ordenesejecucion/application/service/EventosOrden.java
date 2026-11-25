package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import org.slf4j.MDC;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Eventos de integración de M5 con el formato que consumen M2, M6 y M8 (wiki "Contratos REST y Eventos").
 * <p>
 * Sobre: {@code eventId, eventType (= routing key), source, occurredAt, aggregateId, correlationId, payload}.
 * El publicador directo y el outbox usan este mismo formato y el mismo {@code eventId}, así los consumidores
 * descartan el duplicado por su inbox.
 */
public final class EventosOrden {

    public static final String FUENTE = "M5";
    public static final String ORDEN_CREADA = "m5.orden.creada";
    public static final String ORDEN_EJECUTADA = "m5.orden.ejecutada";
    public static final String ORDEN_CANCELADA = "m5.orden.cancelada";
    public static final String ORDEN_RECHAZADA = "m5.orden.rechazada";

    /** Clave del MDC que llena el filtro de correlación de las solicitudes HTTP. */
    private static final String CLAVE_CORRELACION = "correlationId";

    private EventosOrden() {
    }

    /** La misma ejecución conserva su identidad incluso si M5 publica antes de un rollback y vuelve a intentarla. */
    public static UUID idEjecucion(Long ordenId, String brokerExecutionId) {
        String nombre = FUENTE + ":" + ORDEN_EJECUTADA + ":" + ordenId + ":" + brokerExecutionId;
        return UUID.nameUUIDFromBytes(nombre.getBytes(StandardCharsets.UTF_8));
    }

    public static Map<String, Object> sobre(UUID eventId, String tipo, Long ordenId, Map<String, Object> payload) {
        String correlacion = MDC.get(CLAVE_CORRELACION);
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", eventId.toString());
        sobre.put("eventType", tipo);
        sobre.put("source", FUENTE);
        sobre.put("occurredAt", Instant.now().toString());
        sobre.put("aggregateId", String.valueOf(ordenId));
        sobre.put("correlationId", correlacion == null || correlacion.isBlank() ? eventId.toString() : correlacion);
        sobre.put("payload", payload);
        return sobre;
    }

    public static Map<String, Object> creada(Long ordenId, Long usuarioId, String symbol, String lado,
                                             Integer cantidad, String tipoOrden) {
        Map<String, Object> payload = base(ordenId, usuarioId);
        payload.put("symbol", symbol);
        payload.put("side", lado(lado));
        payload.put("qty", cantidad);
        payload.put("type", tipoOrden);
        return payload;
    }

    /**
     * {@code totalAmount} es el efectivo que mueve la ejecución en M2: en una compra, precio por cantidad más la
     * comisión (lo que se confirma de la reserva); en una venta, precio por cantidad menos la comisión (lo que se
     * acredita). {@code executionId} es el id de la ejecución del broker: el mismo que M5 envía por REST a M2 y M6,
     * para que descarten la ejecución si ya la aplicaron por ese camino. {@code finalExecution} indica a M2 cuándo
     * puede cerrar la reserva y liberar el sobrante; una ejecución parcial mantiene reservado el resto.
     * {@code expectedExecutionCount} en el último fill permite esperar los anteriores si REST y RabbitMQ se adelantan.
     */
    public static Map<String, Object> ejecutada(String ejecucionId, Long ordenId, Long usuarioId, String symbol,
                                                String lado, Integer cantidad, BigDecimal precio,
                                                BigDecimal comision, Instant ejecutadaEn, boolean finalExecution,
                                                Integer expectedExecutionCount) {
        String side = lado(lado);
        Instant momento = ejecutadaEn != null ? ejecutadaEn : Instant.now();
        Map<String, Object> fill = new LinkedHashMap<>();
        fill.put("price", precio);
        fill.put("qty", cantidad);
        fill.put("timestamp", momento.toString());

        Map<String, Object> payload = base(ordenId, usuarioId);
        payload.put("executionId", ejecucionId);
        payload.put("symbol", symbol);
        payload.put("side", side);
        payload.put("fills", List.of(fill));
        payload.put("commission", comision != null ? comision : BigDecimal.ZERO);
        payload.put("totalAmount", totalAmount(side, cantidad, precio, comision));
        payload.put("executedAt", momento.toString());
        payload.put("finalExecution", finalExecution);
        if (expectedExecutionCount != null) {
            payload.put("expectedExecutionCount", expectedExecutionCount);
        }
        return payload;
    }

    public static Map<String, Object> cancelada(Long ordenId, Long usuarioId, String motivo) {
        Map<String, Object> payload = base(ordenId, usuarioId);
        payload.put("reason", motivo);
        return payload;
    }

    public static Map<String, Object> rechazada(Long ordenId, Long usuarioId, String motivo, String errorBroker) {
        Map<String, Object> payload = base(ordenId, usuarioId);
        payload.put("reason", motivo);
        payload.put("brokerError", errorBroker != null ? errorBroker : "");
        return payload;
    }

    /** Convierte el lado del dominio (COMPRA/VENTA) al del contrato (BUY/SELL). */
    public static String lado(String lado) {
        if (lado == null) {
            return null;
        }
        return switch (lado.trim().toUpperCase(Locale.ROOT)) {
            case "COMPRA", "BUY" -> "BUY";
            case "VENTA", "SELL" -> "SELL";
            default -> lado;
        };
    }

    public static BigDecimal totalAmount(String side, Integer cantidad, BigDecimal precio, BigDecimal comision) {
        if (cantidad == null || precio == null) {
            return null;
        }
        BigDecimal bruto = precio.multiply(BigDecimal.valueOf(cantidad.longValue()));
        BigDecimal costo = comision != null ? comision : BigDecimal.ZERO;
        BigDecimal neto = "SELL".equals(lado(side)) ? bruto.subtract(costo) : bruto.add(costo);
        return neto.setScale(2, RoundingMode.HALF_UP);
    }

    private static Map<String, Object> base(Long ordenId, Long usuarioId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", String.valueOf(ordenId));
        payload.put("userId", usuarioId);
        return payload;
    }
}
