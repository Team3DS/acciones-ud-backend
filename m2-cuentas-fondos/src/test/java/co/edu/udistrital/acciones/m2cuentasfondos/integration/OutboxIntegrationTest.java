package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OutboxIntegrationTest extends AbstractIntegrationTest {

    private static final String COLA = "m2.outbox.integration";

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepararCola() {
        Queue cola = QueueBuilder.durable(COLA).build();
        amqpAdmin.declareQueue(cola);
        amqpAdmin.declareBinding(BindingBuilder.bind(cola)
                .to(new TopicExchange(RoutingKeys.EXCHANGE))
                .with("m2.#"));
        amqpAdmin.purgeQueue(COLA, true);
    }

    @Test
    void elDepositoGeneraElEventoMovimientoCreado() throws Exception {
        long userId = 9201L;
        String correlacion = "trace-outbox-9201";
        HttpHeaders headers = cabeceras(1L, "ADMINISTRADOR");
        headers.set("Idempotency-Key", "outbox-9201");
        headers.set("X-Correlation-ID", correlacion);

        ResponseEntity<Map> deposito = rest.exchange("/api/v1/funds/deposits", HttpMethod.POST,
                new HttpEntity<>(Map.of("userId", userId, "amount", new BigDecimal("250.00")), headers),
                Map.class);

        assertThat(deposito.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long movementId = ((Number) deposito.getBody().get("movementId")).longValue();

        Message mensaje = esperarMensaje(correlacion);
        Map<String, Object> sobre = objectMapper.readValue(mensaje.getBody(), Map.class);

        assertThat(sobre.get("eventType")).isEqualTo(RoutingKeys.MOVIMIENTO_CREADO);
        assertThat(sobre.get("correlationId")).isEqualTo(correlacion);
        assertThat(sobre.get("aggregateId")).isEqualTo(String.valueOf(movementId));
        assertThat(mensaje.getMessageProperties().getMessageId()).isEqualTo(sobre.get("eventId"));
        assertThat(mensaje.getMessageProperties().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        Object correlacionRecibida = mensaje.getMessageProperties().getHeader("X-Correlation-ID");
        assertThat(correlacionRecibida).isEqualTo(correlacion);

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) sobre.get("payload");
        assertThat(((Number) payload.get("movementId")).longValue()).isEqualTo(movementId);
        assertThat(((Number) payload.get("userId")).longValue()).isEqualTo(userId);
        assertThat(payload.get("type")).isEqualTo("DEPOSITO");
        assertThat(numero(payload.get("amount"))).isEqualByComparingTo("250.00");
        assertThat(numero(payload.get("balance"))).isEqualByComparingTo("250.00");

        UUID eventId = UUID.fromString((String) sobre.get("eventId"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(
                jdbcTemplate.queryForObject("select publicado from outbox_evento where id = ?",
                        Boolean.class, eventId)).isTrue());
    }

    private Message esperarMensaje(String correlacion) {
        AtomicReference<Message> recibido = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(200)).until(() -> {
            Message mensaje = rabbitTemplate.receive(COLA, 1000);
            if (mensaje == null) {
                return false;
            }
            Map<?, ?> sobre = objectMapper.readValue(mensaje.getBody(), Map.class);
            if (correlacion.equals(sobre.get("correlationId"))) {
                recibido.set(mensaje);
                return true;
            }
            return false;
        });
        return recibido.get();
    }

    private HttpHeaders cabeceras(Long userId, String... roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TokenTestFactory.token(userId, roles));
        return headers;
    }

    private BigDecimal numero(Object valor) {
        return new BigDecimal(valor.toString());
    }
}
