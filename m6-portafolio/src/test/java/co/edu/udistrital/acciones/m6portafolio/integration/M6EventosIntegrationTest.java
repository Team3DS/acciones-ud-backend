package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "spring.rabbitmq.listener.simple.retry.initial-interval=100ms")
class M6EventosIntegrationTest extends AbstractIntegrationTest {

    private static final long[] USUARIOS = {7001L, 7002L, 7003L, 7004L, 7005L, 7006L, 7007L};
    private static final Instant EJECUTADO_EN = Instant.parse("2026-06-01T14:00:00Z");

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepararColas() {
        TopicExchange exchange = new TopicExchange(RoutingKeys.EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(RoutingKeys.EXCHANGE_DLX, true, false);
        Queue cola = QueueBuilder.durable(RoutingKeys.COLA_POSICIONES)
                .deadLetterExchange(RoutingKeys.EXCHANGE_DLX)
                .deadLetterRoutingKey(RoutingKeys.COLA_POSICIONES_DLQ)
                .build();
        Queue dlq = QueueBuilder.durable(RoutingKeys.COLA_POSICIONES_DLQ).build();

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareExchange(dlx);
        amqpAdmin.declareQueue(cola);
        amqpAdmin.declareQueue(dlq);
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_EJECUTADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_CANCELADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_RECHAZADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(dlq).to(dlx)
                .with(RoutingKeys.COLA_POSICIONES_DLQ));

        amqpAdmin.purgeQueue(RoutingKeys.COLA_POSICIONES, true);
        amqpAdmin.purgeQueue(RoutingKeys.COLA_POSICIONES_DLQ, true);
    }

    @AfterEach
    void limpiarEscenario() {
        String marcadores = String.join(",", java.util.Collections.nCopies(USUARIOS.length, "?"));
        jdbc.update("""
                delete from outbox_evento where aggregate_id in (
                    select cast(id as varchar) from posicion where user_id in (%s)
                )
                """.formatted(marcadores), argumentos());
        jdbc.update("""
                delete from movimiento_posicion where posicion_id in (
                    select id from posicion where user_id in (%s)
                )
                """.formatted(marcadores), argumentos());
        jdbc.update("""
                delete from reserva_posicion where posicion_id in (
                    select id from posicion where user_id in (%s)
                )
                """.formatted(marcadores), argumentos());
        jdbc.update("delete from posicion where user_id in (%s)".formatted(marcadores), argumentos());
    }

    @Test
    void unaOrdenEjecutadaConDatosDePosicionAplicaLaCompra() {
        long userId = 7001L;
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "ORD-7001", Map.of(
                "orderId", "ORD-7001",
                "executionId", "EXE-7001",
                "fills", List.of(fill("100.00", "10")),
                "totalAmount", new BigDecimal("1000.00"),
                "userId", userId,
                "symbol", "AAPL",
                "side", "BUY"));

        esperarEventoProcesado(eventoId);
        assertThat(cantidad(userId, "AAPL")).isEqualByComparingTo("10");
        assertThat(movimientos("EXE:EXE-7001")).isEqualTo(1);
    }

    @Test
    void elMismoEventoDosVecesDejaUnSoloMovimiento() {
        long userId = 7002L;
        String eventoId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "orderId", "ORD-7002",
                "executionId", "EXE-7002",
                "fills", List.of(fill("50.00", "8")),
                "userId", userId,
                "symbol", "MSFT",
                "side", "BUY");

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "ORD-7002", payload);
        esperarEventoProcesado(eventoId);
        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "ORD-7002", payload);

        await().atMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(movimientos("EXE:EXE-7002")).isEqualTo(1));
        assertThat(cantidad(userId, "MSFT")).isEqualByComparingTo("8");
        assertThat(eventosProcesados(eventoId)).isEqualTo(1);
    }

    @Test
    void unEventoPosteriorAlRestConElMismoExecutionIdNoCambiaNada() {
        long userId = 7003L;
        comprar(userId, "ORD-7003", "EXE-7003", "NVDA", "10", "180.00");
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "ORD-7003", Map.of(
                "orderId", "ORD-7003",
                "executionId", "EXE-7003",
                "fills", List.of(fill("180.00", "10")),
                "userId", userId,
                "symbol", "NVDA",
                "side", "BUY"));

        esperarEventoProcesado(eventoId);
        assertThat(cantidad(userId, "NVDA")).isEqualByComparingTo("10");
        assertThat(movimientos("EXE:EXE-7003")).isEqualTo(1);
    }

    @Test
    void unaVentaSinDatosOpcionalesUsaLaReservaPendiente() {
        long userId = 7004L;
        comprar(userId, "ORD-7004", "EXE-COMPRA-7004", "TSLA", "10", "100.00");
        reservar(userId, "ORD-7004-R", "TSLA", "4", "clave-7004");
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "ORD-7004-R", Map.of(
                "orderId", "ORD-7004-R",
                "executionId", "EXE-VENTA-7004",
                "fills", List.of(fill("120.00", "4")),
                "totalAmount", new BigDecimal("480.00")));

        esperarEventoProcesado(eventoId);
        assertThat(cantidad(userId, "TSLA")).isEqualByComparingTo("6");
        assertThat(reservado(userId, "TSLA")).isEqualByComparingTo("0");
        assertThat(estadoReserva("ORD-7004-R")).isEqualTo("CONFIRMED");
        assertThat(movimientos("EXE:EXE-VENTA-7004")).isEqualTo(1);
    }

    @Test
    void unaOrdenCanceladaLiberaLaReserva() {
        long userId = 7005L;
        comprar(userId, "ORD-7005", "EXE-7005", "AMD", "10", "120.00");
        reservar(userId, "ORD-7005-R", "AMD", "4", "clave-7005");
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_CANCELADA, eventoId, "ORD-7005-R", Map.of(
                "orderId", "ORD-7005-R",
                "reason", "Cancelada por el usuario"));

        esperarEventoProcesado(eventoId);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(estadoReserva("ORD-7005-R")).isEqualTo("RELEASED"));
        assertThat(reservado(userId, "AMD")).isEqualByComparingTo("0");
        assertThat(cantidad(userId, "AMD")).isEqualByComparingTo("10");
        assertThat(motivoLiberacion("ORD-7005-R")).isEqualTo("Cancelada por el usuario");
    }

    @Test
    void unJsonInvalidoTerminaEnLaColaDeMensajesMuertos() {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Message mensaje = new Message("esto no es json".getBytes(StandardCharsets.UTF_8), propiedades);

        rabbitTemplate.send(RoutingKeys.EXCHANGE, RoutingKeys.M5_ORDEN_CANCELADA, mensaje);

        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            Message enDlq = rabbitTemplate.receive(RoutingKeys.COLA_POSICIONES_DLQ, 200);
            if (enDlq != null) {
                recibido.set(enDlq);
            }
            assertThat(recibido.get()).isNotNull();
        });
        assertThat(new String(recibido.get().getBody(), StandardCharsets.UTF_8)).isEqualTo("esto no es json");
    }

    @Test
    void unSobreSinEventIdTerminaEnLaColaDeMensajesMuertos() {
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventType", RoutingKeys.M5_ORDEN_CANCELADA);
        sobre.put("payload", Map.of("orderId", "ORD-7007"));

        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.M5_ORDEN_CANCELADA, sobre);

        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            Message enDlq = rabbitTemplate.receive(RoutingKeys.COLA_POSICIONES_DLQ, 200);
            if (enDlq != null) {
                recibido.set(enDlq);
            }
            assertThat(recibido.get()).isNotNull();
        });
        assertThat(new String(recibido.get().getBody(), StandardCharsets.UTF_8)).contains("ORD-7007");
    }

    private void publicar(String routingKey, String eventoId, String aggregateId, Map<String, Object> payload) {
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", eventoId);
        sobre.put("eventType", routingKey);
        sobre.put("occurredAt", Instant.now().toString());
        sobre.put("aggregateId", aggregateId);
        sobre.put("correlationId", "trace-" + eventoId);
        sobre.put("payload", payload);

        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, sobre);
    }

    private void esperarEventoProcesado(String eventoId) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100))
                .untilAsserted(() -> assertThat(eventosProcesados(eventoId)).isEqualTo(1));
    }

    private int eventosProcesados(String eventoId) {
        return jdbc.queryForObject("select count(*) from evento_procesado where event_id = ?",
                Integer.class, eventoId);
    }

    private int movimientos(String clave) {
        return jdbc.queryForObject("select count(*) from movimiento_posicion where idempotency_key = ?",
                Integer.class, clave);
    }

    private BigDecimal cantidad(long userId, String symbol) {
        List<BigDecimal> valores = jdbc.queryForList(
                "select cantidad from posicion where user_id = ? and symbol = ?", BigDecimal.class, userId, symbol);
        return valores.isEmpty() ? null : valores.get(0);
    }

    private BigDecimal reservado(long userId, String symbol) {
        return jdbc.queryForObject(
                "select cantidad_reservada from posicion where user_id = ? and symbol = ?",
                BigDecimal.class, userId, symbol);
    }

    private String estadoReserva(String orderId) {
        return jdbc.queryForObject("select estado from reserva_posicion where order_id = ?",
                String.class, orderId);
    }

    private String motivoLiberacion(String orderId) {
        return jdbc.queryForObject("select motivo_liberacion from reserva_posicion where order_id = ?",
                String.class, orderId);
    }

    private void comprar(long userId, String orderId, String executionId, String symbol,
                         String quantity, String price) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("side", "BUY");
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", EJECUTADO_EN.toString());

        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(userId, null)), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void reservar(long userId, String orderId, String symbol, String quantity, String clave) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("idempotencyKey", clave);

        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/positions/reservations", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(userId, clave)), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static Map<String, Object> fill(String precio, String cantidad) {
        return Map.of("price", new BigDecimal(precio), "qty", new BigDecimal(cantidad),
                "timestamp", EJECUTADO_EN.toString());
    }

    private static HttpHeaders cabeceras(long userId, String claveIdempotencia) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (claveIdempotencia != null) {
            headers.set("Idempotency-Key", claveIdempotencia);
        }
        return headers;
    }

    private static Object[] argumentos() {
        Object[] valores = new Object[USUARIOS.length];
        for (int i = 0; i < USUARIOS.length; i++) {
            valores[i] = USUARIOS[i];
        }
        return valores;
    }
}
