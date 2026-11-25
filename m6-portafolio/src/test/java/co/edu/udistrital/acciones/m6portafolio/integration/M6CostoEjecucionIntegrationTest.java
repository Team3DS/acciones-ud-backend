package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** El coste de una ejecución es histórico; no es el coste de la posición después de venderla. */
class M6CostoEjecucionIntegrationTest extends AbstractIntegrationTest {

    private static final Instant EJECUTADO_EN = Instant.parse("2026-11-24T15:00:00Z");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private ObjectMapper json;

    @Test
    void ventaTotalRestConservaElCosteYLasRepeticionesNoLoRecalculanDesdeUnaPosicionNueva() throws Exception {
        long userId = 86001;
        String ventaId = "COST-86001-SELL";
        ResponseEntity<Map> compra = aplicarRest(userId, 8600101, "COST-86001-BUY", "BUY", "10", "123.45");
        assertThat(compra.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(numero(compra.getBody(), "averageCost")).isEqualByComparingTo("123.45");
        reservar(userId, 8600102, "10");

        ResponseEntity<Map> venta = aplicarRest(userId, 8600102, ventaId, "SELL", "10", "150.00");
        assertThat(venta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(venta.getBody().get("applied")).isEqualTo(true);
        assertThat(numero(venta.getBody(), "averageCost")).isEqualByComparingTo("123.45");
        Map<?, ?> posicionRespuesta = (Map<?, ?>) venta.getBody().get("position");
        assertThat(numero(posicionRespuesta, "quantity")).isEqualByComparingTo("0");
        assertThat(numero(posicionRespuesta, "averageCost")).isEqualByComparingTo("0");
        comprobarPosicion(userId, "0", "0", "265.50");
        comprobarRegistroVenta(ventaId, "123.45");

        // Una adquisición posterior cambia el coste actual, pero no el de la venta que se reintenta.
        assertThat(aplicarRest(userId, 8600103, "COST-86001-NEW-BUY", "BUY", "5", "50.00")
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        publicarVenta(userId, 8600102, ventaId, "10", "150.00");
        ResponseEntity<Map> repetida = aplicarRest(userId, 8600102, ventaId, "SELL", "10", "150.00");
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repetida.getBody().get("applied")).isEqualTo(false);
        assertThat(numero(repetida.getBody(), "averageCost")).isEqualByComparingTo("123.45");
        assertThat(numero((Map<?, ?>) repetida.getBody().get("position"), "averageCost"))
                .isEqualByComparingTo("50.00");
        comprobarPosicion(userId, "5", "50.00", "265.50");
        comprobarRegistroVenta(ventaId, "123.45");
    }

    @Test
    void ventaTotalRabbitConservaElCosteAlRepetirsePorRestSinNuevaSalidaNiMovimiento() throws Exception {
        long userId = 86002;
        String ventaId = "COST-86002-SELL";
        assertThat(aplicarRest(userId, 8600201, "COST-86002-BUY", "BUY", "10", "210.00")
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        publicarVenta(userId, 8600202, ventaId, "10", "225.00");
        comprobarPosicion(userId, "0", "0", "150.00");
        comprobarRegistroVenta(ventaId, "210.00");

        ResponseEntity<Map> repetida = aplicarRest(userId, 8600202, ventaId, "SELL", "10", "225.00");
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repetida.getBody().get("applied")).isEqualTo(false);
        assertThat(numero(repetida.getBody(), "averageCost")).isEqualByComparingTo("210.00");
        assertThat(numero((Map<?, ?>) repetida.getBody().get("position"), "averageCost"))
                .isEqualByComparingTo("0");
        comprobarPosicion(userId, "0", "0", "150.00");
        comprobarRegistroVenta(ventaId, "210.00");
    }

    private ResponseEntity<Map> aplicarRest(long userId, long orderId, String executionId,
                                            String side, String cantidad, String precio) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("orderId", Long.toString(orderId));
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", "AAPL");
        cuerpo.put("side", side);
        cuerpo.put("quantity", new BigDecimal(cantidad));
        cuerpo.put("price", new BigDecimal(precio));
        cuerpo.put("executedAt", EJECUTADO_EN.toString());
        return rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, headers(userId)), Map.class);
    }

    private void reservar(long userId, long orderId, String cantidad) {
        var respuesta = rest.exchange("/api/v1/positions/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", Long.toString(orderId), "userId", userId,
                        "symbol", "AAPL", "quantity", new BigDecimal(cantidad),
                        "idempotencyKey", "cost-reserva-" + userId), headers(userId)), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private void publicarVenta(long userId, long orderId, String executionId, String cantidad, String precio) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of("orderId", Long.toString(orderId), "executionId", executionId,
                "userId", userId, "symbol", "AAPL", "side", "SELL", "fills", List.of(Map.of(
                        "qty", new BigDecimal(cantidad), "price", new BigDecimal(precio),
                        "timestamp", EJECUTADO_EN.toString())));
        rabbit.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.M5_ORDEN_EJECUTADA,
                Map.of("eventId", eventId, "eventType", RoutingKeys.M5_ORDEN_EJECUTADA,
                        "aggregateId", Long.toString(orderId), "occurredAt", EJECUTADO_EN.toString(),
                        "correlationId", "cost-" + eventId, "payload", payload));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("select count(*) from evento_procesado where event_id = ?",
                        Integer.class, eventId)).isEqualTo(1));
    }

    private void comprobarRegistroVenta(String executionId, String coste) throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from ejecucion_aplicada where ejecucion_orden_id = ?",
                Integer.class, executionId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select precio_promedio_costo from ejecucion_aplicada "
                        + "where ejecucion_orden_id = ?", BigDecimal.class, executionId))
                .isEqualByComparingTo(coste).isPositive();
        assertThat(jdbc.queryForObject("select count(*) from movimiento_posicion "
                        + "where execution_id = ? and tipo = 'VENTA'", Integer.class, executionId))
                .isEqualTo(1);
        var salidas = jdbc.queryForList("select payload from outbox_evento "
                        + "where event_type = ? and payload::jsonb ->> 'executionId' = ?",
                String.class, RoutingKeys.POSICIONES_ACTUALIZADAS, executionId);
        assertThat(salidas).hasSize(1);
        JsonNode evento = json.readTree(salidas.getFirst());
        assertThat(evento.path("averageCost").decimalValue()).isEqualByComparingTo(coste).isPositive();
    }

    private void comprobarPosicion(long userId, String cantidad, String coste, String pnl) {
        Map<String, Object> posicion = jdbc.queryForMap("select cantidad, costo_promedio, pnl_realizado "
                + "from posicion where user_id = ? and symbol = 'AAPL'", userId);
        assertThat((BigDecimal) posicion.get("cantidad")).isEqualByComparingTo(cantidad);
        assertThat((BigDecimal) posicion.get("costo_promedio")).isEqualByComparingTo(coste);
        assertThat((BigDecimal) posicion.get("pnl_realizado")).isEqualByComparingTo(pnl);
    }

    private static BigDecimal numero(Map<?, ?> mapa, String campo) {
        assertThat(mapa.get(campo)).as(campo).isNotNull();
        return new BigDecimal(mapa.get(campo).toString());
    }

    private static HttpHeaders headers(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        return headers;
    }
}
