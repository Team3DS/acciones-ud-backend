package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ValorarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import org.junit.jupiter.api.AfterEach;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class M6FlujoCompletoIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 6901L;
    private static final String SYMBOL = "FLOW";
    private static final long ORDER_COMPRA_REST = 690101L;
    private static final long ORDER_COMPRA_EVENTO = 690102L;
    private static final long ORDER_RESERVA = 690103L;
    private static final String PREFIJO_EVENTO = "flow-";

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ValorarPosicionesUseCase valorarPosiciones;

    private final String token = TokenTestFactory.token(USER_ID, "INVERSIONISTA");

    @AfterEach
    void limpiarEscenario() {
        jdbc.update("""
                delete from outbox_evento where aggregate_id in (
                    select cast(id as varchar) from posicion where user_id = ?
                )
                """, USER_ID);
        jdbc.update("""
                delete from movimiento_posicion where posicion_id in (
                    select id from posicion where user_id = ?
                )
                """, USER_ID);
        jdbc.update("""
                delete from reserva_posicion where posicion_id in (
                    select id from posicion where user_id = ?
                )
                """, USER_ID);
        jdbc.update("delete from posicion where user_id = ?", USER_ID);
        jdbc.update("delete from evento_procesado where event_id like ?", PREFIJO_EVENTO + "%");
    }

    @Test
    void elFlujoCompletoMantieneElDisponibleYTerminaEnElPortafolioValorado() {
        comprarPorRest("10", "180.00", "EXE-FLOW-REST");
        Map<?, ?> posicion = posicion();
        assertPosicion(posicion, "10", "0", "10");
        assertThat(numero(posicion, "averageCost")).isEqualByComparingTo("180.0000");

        String eventoCompra = publicarEjecutada(ORDER_COMPRA_EVENTO, "EXE-FLOW-EVENTO",
                List.of(fill("190.00", "10")));
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(numero(posicion(), "quantity")).isEqualByComparingTo("20"));
        assertThat(eventoProcesado(eventoCompra)).isEqualTo(1);
        posicion = posicion();
        assertPosicion(posicion, "20", "0", "20");
        assertThat(numero(posicion, "averageCost")).isEqualByComparingTo("185.0000");

        ResponseEntity<Map> reserva = reservar("8", "FLOW-KEY-1");
        assertThat(reserva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long reservaId = ((Number) reserva.getBody().get("reservationId")).longValue();
        posicion = posicion();
        assertPosicion(posicion, "20", "8", "12");

        ResponseEntity<Map> venta = actualizar(ORDER_RESERVA, "EXE-FLOW-VENTA-1", "SELL", "5", "200.00");
        assertThat(venta.getStatusCode()).isEqualTo(HttpStatus.OK);
        posicion = posicion();
        assertPosicion(posicion, "15", "3", "12");
        assertThat(numero(posicion, "realizedPnl")).isEqualByComparingTo("75.00");

        String eventoCancelacion = publicarCancelada(ORDER_RESERVA, "Cancelada durante el flujo");
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(estadoReserva(reservaId)).isEqualTo("LIBERADA"));
        assertThat(eventoProcesado(eventoCancelacion)).isEqualTo(1);
        posicion = posicion();
        assertPosicion(posicion, "15", "0", "15");

        prepararMercadoConPrecio("210.00");
        assertThat(valorarPosiciones.ejecutar()).isGreaterThanOrEqualTo(1);
        posicion = posicion();
        assertPosicion(posicion, "15", "0", "15");
        assertThat(numero(posicion, "currentPrice")).isEqualByComparingTo("210.00");
        assertThat(numero(posicion, "marketValue")).isEqualByComparingTo("3150.00");
        assertThat(numero(posicion, "unrealizedPnl")).isEqualByComparingTo("375.00");
        assertThat(posicion.get("stale")).isEqualTo(false);

        Map<?, ?> portafolio = portafolio();
        assertThat(((List<?>) portafolio.get("positions"))).hasSize(1);
        assertThat(numero(portafolio, "totalValue")).isEqualByComparingTo("3150.00");
        assertThat(numero(portafolio, "totalCost")).isEqualByComparingTo("2775.00");
        assertThat(numero(portafolio, "unrealizedPnl")).isEqualByComparingTo("375.00");
        assertThat(numero(portafolio, "realizedPnl")).isEqualByComparingTo("75.00");
        assertThat(portafolio.get("stale")).isEqualTo(false);

        Map<?, ?> posicionFinal = (Map<?, ?>) ((List<?>) portafolio.get("positions")).get(0);
        assertThat(numero(posicionFinal, "availableQuantity"))
                .isEqualByComparingTo(numero(posicionFinal, "quantity")
                        .subtract(numero(posicionFinal, "reservedQuantity")));
    }

    private void comprarPorRest(String quantity, String price, String executionId) {
        ResponseEntity<Map> respuesta = actualizar(ORDER_COMPRA_REST, executionId, "BUY", quantity, price);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().get("applied")).isEqualTo(true);
    }

    private ResponseEntity<Map> actualizar(long orderId, String executionId, String side,
                                           String quantity, String price) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", USER_ID);
        cuerpo.put("symbol", SYMBOL);
        cuerpo.put("side", side);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", Instant.now().toString());
        return rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras()), Map.class);
    }

    private ResponseEntity<Map> reservar(String quantity, String clave) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", ORDER_RESERVA);
        cuerpo.put("userId", USER_ID);
        cuerpo.put("symbol", SYMBOL);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("idempotencyKey", clave);
        return rest.exchange("/api/v1/positions/reservations", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras()), Map.class);
    }

    private Map<?, ?> posicion() {
        return rest.exchange("/api/v1/positions/" + USER_ID + "/" + SYMBOL, HttpMethod.GET,
                new HttpEntity<>(cabeceras()), Map.class).getBody();
    }

    private Map<?, ?> portafolio() {
        return rest.exchange("/api/v1/portfolio/" + USER_ID, HttpMethod.GET,
                new HttpEntity<>(cabeceras()), Map.class).getBody();
    }

    private String publicarEjecutada(long orderId, String executionId, List<Map<String, Object>> fills) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("executionId", executionId);
        payload.put("fills", fills);
        payload.put("totalAmount", new BigDecimal("1900.00"));
        payload.put("userId", USER_ID);
        payload.put("symbol", SYMBOL);
        payload.put("side", "BUY");
        return publicar(RoutingKeys.M5_ORDEN_EJECUTADA, String.valueOf(orderId), payload);
    }

    private String publicarCancelada(long orderId, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("reason", reason);
        return publicar(RoutingKeys.M5_ORDEN_CANCELADA, String.valueOf(orderId), payload);
    }

    private String publicar(String routingKey, String aggregateId, Map<String, Object> payload) {
        String eventoId = PREFIJO_EVENTO + UUID.randomUUID();
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", eventoId);
        sobre.put("eventType", routingKey);
        sobre.put("occurredAt", Instant.now().toString());
        sobre.put("aggregateId", aggregateId);
        sobre.put("correlationId", "trace-" + eventoId);
        sobre.put("payload", payload);
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, sobre);
        return eventoId;
    }

    private void prepararMercadoConPrecio(String price) {
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/market/status"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"mercadoMic\":\"XNYS\",\"estado\":\"ABIERTO\","
                                + "\"consultadoEn\":\"" + Instant.now() + "\"}")));
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("[{\"symbol\":\"" + SYMBOL + "\",\"price\":" + price
                                + ",\"timestamp\":\"" + Instant.now() + "\"}]")));
    }

    private int eventoProcesado(String eventoId) {
        return jdbc.queryForObject("select count(*) from evento_procesado where event_id = ?",
                Integer.class, eventoId);
    }

    private String estadoReserva(long reservaId) {
        return jdbc.queryForObject("select estado_reserva from reserva_posicion where id = ?",
                String.class, reservaId);
    }

    private static Map<String, Object> fill(String price, String quantity) {
        return Map.of("price", new BigDecimal(price), "qty", new BigDecimal(quantity),
                "timestamp", Instant.now().toString());
    }

    private static void assertPosicion(Map<?, ?> posicion, String quantity, String reserved,
                                       String available) {
        assertThat(numero(posicion, "quantity")).isEqualByComparingTo(quantity);
        assertThat(numero(posicion, "reservedQuantity")).isEqualByComparingTo(reserved);
        assertThat(numero(posicion, "availableQuantity")).isEqualByComparingTo(available);
        assertThat(numero(posicion, "availableQuantity"))
                .isEqualByComparingTo(numero(posicion, "quantity")
                        .subtract(numero(posicion, "reservedQuantity")));
    }

    private static BigDecimal numero(Map<?, ?> mapa, String clave) {
        Object valor = mapa.get(clave);
        if (valor instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        return new BigDecimal(valor.toString());
    }

    private HttpHeaders cabeceras() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
