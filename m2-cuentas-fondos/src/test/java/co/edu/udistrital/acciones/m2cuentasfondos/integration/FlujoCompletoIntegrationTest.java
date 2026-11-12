package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class FlujoCompletoIntegrationTest extends AbstractIntegrationTest {

    private static final long USER_ID = 9951L;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void prepararColas() {
        TopicExchange exchange = new TopicExchange(RoutingKeys.EXCHANGE, true, false);
        Queue cola = QueueBuilder.durable(RoutingKeys.COLA_MOVIMIENTOS)
                .deadLetterExchange(RoutingKeys.EXCHANGE_DLX)
                .deadLetterRoutingKey(RoutingKeys.COLA_MOVIMIENTOS_DLQ)
                .build();

        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(cola);
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_EJECUTADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_CANCELADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M5_ORDEN_RECHAZADA));
        amqpAdmin.declareBinding(BindingBuilder.bind(cola).to(exchange)
                .with(RoutingKeys.M10_SUSCRIPCION_ACTIVADA));
        amqpAdmin.purgeQueue(RoutingKeys.COLA_MOVIMIENTOS, true);
        amqpAdmin.purgeQueue(RoutingKeys.COLA_MOVIMIENTOS_DLQ, true);
    }

    @Test
    void elFlujoCompletoConservaTotalIgualADisponibleMasReservado() {
        Map<String, Object> deposito = depositar(new BigDecimal("1000.00"), "flujo-dep-9951");
        assertConservacion(deposito);
        assertThat(numero(deposito.get("available"))).isEqualByComparingTo("1000.00");

        long primeraReserva = reservar("orden-flujo-9951", new BigDecimal("300.00"), "flujo-res-9951");
        Map<String, Object> trasReserva = saldo();
        assertConservacion(trasReserva);
        assertThat(numero(trasReserva.get("available"))).isEqualByComparingTo("700.00");
        assertThat(numero(trasReserva.get("reserved"))).isEqualByComparingTo("300.00");

        publicarEjecucionParcial("orden-flujo-9951", "ejecucion-flujo-9951", new BigDecimal("290.50"));
        esperarEstadoReserva(primeraReserva, "CONFIRMED");
        esperarSaldo("709.50", "0.00");
        Map<String, Object> trasEjecucion = saldo();
        assertConservacion(trasEjecucion);
        assertThat(numero(trasEjecucion.get("total"))).isEqualByComparingTo("709.50");

        Map<String, Object> retiro = retirar(new BigDecimal("100.00"), "flujo-ret-9951");
        assertConservacion(retiro);
        assertThat(numero(retiro.get("available"))).isEqualByComparingTo("609.50");
        assertThat(numero(retiro.get("total"))).isEqualByComparingTo("609.50");

        long segundaReserva = reservar("orden-flujo-9951-b", new BigDecimal("200.00"), "flujo-res-9951-b");
        Map<String, Object> trasSegundaReserva = saldo();
        assertConservacion(trasSegundaReserva);
        assertThat(numero(trasSegundaReserva.get("available"))).isEqualByComparingTo("409.50");
        assertThat(numero(trasSegundaReserva.get("reserved"))).isEqualByComparingTo("200.00");
        assertThat(numero(trasSegundaReserva.get("total"))).isEqualByComparingTo("609.50");

        publicarCancelacion("orden-flujo-9951-b", "CanceladaFlujoCompleto");
        esperarEstadoReserva(segundaReserva, "RELEASED");
        esperarSaldo("609.50", "0.00");
        Map<String, Object> trasLiberacion = saldo();
        assertConservacion(trasLiberacion);
        assertThat(numero(trasLiberacion.get("total"))).isEqualByComparingTo("609.50");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class,
                "CNF:" + primeraReserva)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from movimiento where idempotency_key = ?", Long.class,
                "LIB:" + segundaReserva)).isEqualTo(1L);
    }

    private Map<String, Object> depositar(BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(1L, "ADMINISTRADOR");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", USER_ID, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody();
    }

    private Map<String, Object> retirar(BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(USER_ID, "INVERSIONISTA");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/withdrawals", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", USER_ID, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody();
    }

    private long reservar(String orden, BigDecimal monto, String clave) {
        HttpHeaders headers = cabeceras(USER_ID, "INVERSIONISTA");
        headers.set("Idempotency-Key", clave);
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/funds/reservations", HttpMethod.POST,
                new HttpEntity<>(Map.of("orderId", orden, "userId", USER_ID, "amount", monto), headers), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) respuesta.getBody().get("reservationId")).longValue();
    }

    private Map<String, Object> saldo() {
        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/accounts/" + USER_ID + "/balance",
                HttpMethod.GET, new HttpEntity<>(cabeceras(USER_ID, "INVERSIONISTA")), Map.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private void publicarEjecucionParcial(String orden, String ejecucion, BigDecimal monto) {
        publicar(RoutingKeys.M5_ORDEN_EJECUTADA, UUID.randomUUID().toString(), "flujo-ejecucion-9951", orden,
                Map.of("orderId", orden, "executionId", ejecucion, "fills", List.of(),
                        "totalAmount", monto, "side", "BUY", "userId", USER_ID));
    }

    private void publicarCancelacion(String orden, String motivo) {
        publicar(RoutingKeys.M5_ORDEN_CANCELADA, UUID.randomUUID().toString(), "flujo-cancelacion-9951", orden,
                Map.of("orderId", orden, "reason", motivo));
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

    private void esperarSaldo(String disponible, String reservado) {
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            Map<String, Object> actual = saldo();
            assertThat(numero(actual.get("available"))).isEqualByComparingTo(disponible);
            assertThat(numero(actual.get("reserved"))).isEqualByComparingTo(reservado);
        });
    }

    private void assertConservacion(Map<String, Object> saldo) {
        BigDecimal disponible = numero(saldo.get("available"));
        BigDecimal reservado = numero(saldo.get("reserved"));
        assertThat(numero(saldo.get("total"))).isEqualByComparingTo(disponible.add(reservado));
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
