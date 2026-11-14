package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "spring.rabbitmq.listener.simple.retry.initial-interval=100ms")
class EventosEntrantesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararColas() {
        TopicExchange exchange = new TopicExchange(RoutingKeys.EXCHANGE, true, false);
        TopicExchange dlx = new TopicExchange(RoutingKeys.EXCHANGE_DLX, true, false);
        Queue cola = QueueBuilder.durable(RoutingKeys.COLA_MOVIMIENTOS)
                .deadLetterExchange(RoutingKeys.EXCHANGE_DLX)
                .deadLetterRoutingKey(RoutingKeys.COLA_MOVIMIENTOS_DLQ)
                .build();
        Queue dlq = QueueBuilder.durable(RoutingKeys.COLA_MOVIMIENTOS_DLQ).build();

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
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M10_SUSCRIPCION_ACTIVADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(dlq).to(dlx)
                .with(RoutingKeys.COLA_MOVIMIENTOS_DLQ));

        amqpAdmin.purgeQueue(RoutingKeys.COLA_MOVIMIENTOS, true);
        amqpAdmin.purgeQueue(RoutingKeys.COLA_MOVIMIENTOS_DLQ, true);
    }

    @Test
    void unaOrdenEjecutadaDeCompraConfirmaLaReserva() {
        long userId = 9801L;
        depositar(userId, new BigDecimal("1000.00"), "evt-dep-9801");
        long reservaId = reservar(userId, "orden-evt-9801", new BigDecimal("300.00"), "evt-res-9801");
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "trace-evt-compra-9801",
                "orden-evt-9801", Map.of(
                        "orderId", "orden-evt-9801",
                        "executionId", "exec-evt-9801",
                        "fills", List.of(),
                        "totalAmount", new BigDecimal("290.50"),
                        "side", "BUY",
                        "userId", userId));

        esperarEstadoReserva(reservaId, "CONFIRMED");
        esperarEventoProcesado(eventoId);

        Map<String, Object> cuenta = jdbcTemplate.queryForMap(
                "select saldo_disponible, saldo_reservado, saldo_contable from cuenta where user_id = ?", userId);
        assertThat(numero(cuenta.get("saldo_disponible"))).isEqualByComparingTo("709.50");
        assertThat(numero(cuenta.get("saldo_reservado"))).isEqualByComparingTo("0.00");
        assertThat(numero(cuenta.get("saldo_contable"))).isEqualByComparingTo("709.50");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class, "CNF:" + reservaId))
                .isEqualTo(1L);
    }

    @Test
    void unEventoDuplicadoGeneraUnSoloMovimiento() {
        long userId = 9802L;
        depositar(userId, new BigDecimal("1000.00"), "evt-dep-9802");
        long reservaId = reservar(userId, "orden-evt-9802", new BigDecimal("300.00"), "evt-res-9802");
        String eventoId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "orderId", "orden-evt-9802",
                "executionId", "exec-evt-9802",
                "fills", List.of(),
                "totalAmount", new BigDecimal("300.00"),
                "side", "BUY",
                "userId", userId);

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "trace-evt-dup-9802", "orden-evt-9802", payload);
        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "trace-evt-dup-9802", "orden-evt-9802", payload);

        esperarEstadoReserva(reservaId, "CONFIRMED");
        esperarEventoProcesado(eventoId);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class, "CNF:" + reservaId))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from evento_procesado where event_id = ?", Long.class, eventoId)).isEqualTo(1L);
    }

    @Test
    void unaOrdenCanceladaLiberaLaReserva() {
        long userId = 9803L;
        depositar(userId, new BigDecimal("1000.00"), "evt-dep-9803");
        long reservaId = reservar(userId, "orden-evt-9803", new BigDecimal("300.00"), "evt-res-9803");
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_CANCELADA, eventoId, "trace-evt-cancela-9803", "orden-evt-9803",
                Map.of("orderId", "orden-evt-9803", "reason", "OrdenCanceladaPorEvento"));

        esperarEstadoReserva(reservaId, "RELEASED");
        esperarEventoProcesado(eventoId);

        Map<String, Object> cuenta = jdbcTemplate.queryForMap(
                "select saldo_disponible, saldo_reservado, saldo_contable from cuenta where user_id = ?", userId);
        assertThat(numero(cuenta.get("saldo_disponible"))).isEqualByComparingTo("1000.00");
        assertThat(numero(cuenta.get("saldo_reservado"))).isEqualByComparingTo("0.00");
        assertThat(jdbcTemplate.queryForObject(
                "select motivo_liberacion from reserva where id = ?", String.class, reservaId))
                .isEqualTo("OrdenCanceladaPorEvento");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class, "LIB:" + reservaId))
                .isEqualTo(1L);
    }

    @Test
    void unaVentaConUserIdGeneraUnCreditoVenta() {
        long userId = 9804L;
        String orderId = "orden-evt-9804-venta";
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, eventoId, "trace-evt-venta-9804", orderId, Map.of(
                "orderId", orderId,
                "executionId", "exec-evt-9804",
                "fills", List.of(),
                "totalAmount", new BigDecimal("150.00"),
                "side", "SELL",
                "userId", userId));

        esperarEventoProcesado(eventoId);
        Map<String, Object> movimiento = esperarMovimiento("VTA:" + orderId);
        assertThat(movimiento.get("tipo")).isEqualTo("CREDITO_VENTA");
        assertThat(numero(movimiento.get("monto"))).isEqualByComparingTo("150.00");
        assertThat(numero(movimiento.get("saldo_posterior"))).isEqualByComparingTo("150.00");

        Map<String, Object> cuenta = jdbcTemplate.queryForMap(
                "select saldo_disponible, saldo_reservado, saldo_contable from cuenta where user_id = ?", userId);
        assertThat(numero(cuenta.get("saldo_disponible"))).isEqualByComparingTo("150.00");
        assertThat(numero(cuenta.get("saldo_contable"))).isEqualByComparingTo("150.00");
    }

    @Test
    void unaSuscripcionSeMarcaProcesadaSinTocarElSaldo() {
        long userId = 9815L;
        String eventoId = UUID.randomUUID().toString();

        publicar(RoutingKeys.M10_SUSCRIPCION_ACTIVADA, eventoId, "trace-evt-suscripcion-9815", "suscripcion-9815",
                Map.of("userId", userId, "planId", "PREMIUM", "stripeSubscriptionId", "sub_9815"));

        esperarEventoProcesado(eventoId);

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from cuenta where user_id = ?", Long.class, userId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where cuenta_id in (select id from cuenta where user_id = ?)",
                Long.class, userId)).isZero();
    }

    @Test
    void unJsonInvalidoTerminaEnLaColaDeMensajesMuertos() {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Message mensaje = new Message("esto no es json".getBytes(StandardCharsets.UTF_8), propiedades);

        rabbitTemplate.send(RoutingKeys.EXCHANGE, RoutingKeys.M5_ORDEN_CANCELADA, mensaje);

        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            Message enDlq = rabbitTemplate.receive(RoutingKeys.COLA_MOVIMIENTOS_DLQ, 200);
            if (enDlq != null) {
                recibido.set(enDlq);
            }
            assertThat(recibido.get()).isNotNull();
        });
        assertThat(new String(recibido.get().getBody(), StandardCharsets.UTF_8)).isEqualTo("esto no es json");
    }

    @Test
    void unEventoSinOrderIdTerminaEnLaColaDeMensajesMuertos() {
        String eventoId = UUID.randomUUID().toString();
        publicar(RoutingKeys.M5_ORDEN_CANCELADA, eventoId, "trace-evt-dlq-sin-order", "orden-sin-order",
                Map.of("reason", "SinOrderId"));

        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(100)).untilAsserted(() -> {
            Message enDlq = rabbitTemplate.receive(RoutingKeys.COLA_MOVIMIENTOS_DLQ, 200);
            if (enDlq != null) {
                recibido.set(enDlq);
            }
            assertThat(recibido.get()).isNotNull();
        });
        assertThat(new String(recibido.get().getBody(), StandardCharsets.UTF_8)).contains(eventoId);
    }

    private void publicar(String routingKey, String eventoId, String correlacion, String aggregateId,
                          Map<String, Object> payload) {
        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", eventoId);
        sobre.put("eventType", routingKey);
        sobre.put("occurredAt", Instant.now().toString());
        sobre.put("aggregateId", aggregateId);
        sobre.put("correlationId", correlacion);
        sobre.put("payload", payload);

        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, sobre, mensaje -> {
            mensaje.getMessageProperties().setHeader("X-Correlation-ID", correlacion);
            return mensaje;
        });
    }

    private void esperarEstadoReserva(long reservaId, String estado) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "select estado from reserva where id = ?", String.class, reservaId)).isEqualTo(estado));
    }

    private void esperarEventoProcesado(String eventoId) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from evento_procesado where event_id = ?", Long.class, eventoId))
                        .isEqualTo(1L));
    }

    private Map<String, Object> esperarMovimiento(String clave) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from movimiento where idempotency_key = ?", Long.class, clave))
                        .isEqualTo(1L));
        return jdbcTemplate.queryForMap(
                "select tipo, monto, saldo_posterior from movimiento where idempotency_key = ?", clave);
    }

    private void depositar(long userId, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(1L, "ADMINISTRADOR");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private long reservar(long userId, String orden, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(userId, "INVERSIONISTA");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", orden, "userId", userId, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private HttpHeaders cabeceras(long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}
