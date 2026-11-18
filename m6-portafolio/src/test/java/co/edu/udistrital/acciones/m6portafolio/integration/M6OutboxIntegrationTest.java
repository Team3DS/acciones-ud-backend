package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class M6OutboxIntegrationTest extends AbstractIntegrationTest {

    private static final Instant EJECUTADO_EN = Instant.parse("2026-04-05T14:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TopicExchange accionesExchange;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unaCompraPublicaElEventoConElSobreCompletoYMarcaLasFilas() throws Exception {
        String cola = "m6.prueba." + UUID.randomUUID();
        Queue destino = new Queue(cola, false, false, true);
        amqpAdmin.declareQueue(destino);
        amqpAdmin.declareBinding(BindingBuilder.bind(destino).to(accionesExchange).with("m6.#"));
        try {
            long userId = 6101L;
            String correlacion = "corr-outbox-6101";
            String token = TokenTestFactory.token(userId, "INVERSIONISTA");
            HttpHeaders cabeceras = cabeceras(token);
            cabeceras.set(CorrelationIdFilter.HEADER, correlacion);

            ResponseEntity<Map> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                    new HttpEntity<>(cuerpo(userId, "ORD-6101", "EXE-6101", "AAPL", "10", "180.00"), cabeceras),
                    Map.class);
            assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);

            Message mensaje = rabbitTemplate.receive(cola, 20_000);
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.getMessageProperties().getReceivedRoutingKey())
                    .isEqualTo(RoutingKeys.POSICIONES_ACTUALIZADAS);

            Map<String, Object> fila = jdbc.queryForMap(
                    "select id, publicado, reintentos from outbox_evento where correlation_id = ?", correlacion);
            assertThat(fila.get("id").toString())
                    .isEqualTo(mensaje.getMessageProperties().getMessageId());
            assertThat(fila.get("publicado")).isEqualTo(Boolean.TRUE);
            assertThat(((Number) fila.get("reintentos")).intValue()).isZero();
            assertThat((String) mensaje.getMessageProperties().getHeader(CorrelationIdFilter.HEADER))
                    .isEqualTo(correlacion);
            assertThat(mensaje.getMessageProperties().getContentType())
                    .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(mensaje.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");

            JsonNode sobre = objectMapper.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            assertThat(sobre.get("eventId").asText()).isEqualTo(fila.get("id").toString());
            assertThat(sobre.get("eventType").asText()).isEqualTo(RoutingKeys.POSICIONES_ACTUALIZADAS);
            assertThat(sobre.get("source").asText()).isEqualTo("m6");
            assertThat(sobre.get("aggregateId").asText()).isNotBlank();
            assertThat(sobre.get("correlationId").asText()).isEqualTo(correlacion);
            assertThat(sobre.get("occurredAt").asText()).isNotBlank();

            JsonNode payload = sobre.get("payload");
            assertThat(payload.get("orderId").asText()).isEqualTo("ORD-6101");
            assertThat(payload.get("executionId").asText()).isEqualTo("EXE-6101");
            assertThat(payload.get("userId").asLong()).isEqualTo(userId);
            assertThat(payload.get("symbol").asText()).isEqualTo("AAPL");
            assertThat(payload.get("side").asText()).isEqualTo("BUY");
            assertThat(payload.get("quantity").decimalValue()).isEqualByComparingTo("10");
            assertThat(payload.get("price").decimalValue()).isEqualByComparingTo("180.00");
            assertThat(payload.get("newQuantity").decimalValue()).isEqualByComparingTo("10");
            assertThat(payload.get("averageCost").decimalValue()).isEqualByComparingTo("180.0000");
            assertThat(payload.get("realizedPnl").decimalValue()).isEqualByComparingTo("0");
        } finally {
            amqpAdmin.deleteQueue(cola);
        }
    }

    @Test
    void flywayAplicaLaTablaDeOutbox() {
        Integer tablas = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'outbox_evento'
                """, Integer.class);
        Integer versiones = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '2' and success = true",
                Integer.class);

        assertThat(tablas).isEqualTo(1);
        assertThat(versiones).isEqualTo(1);
    }

    private static Map<String, Object> cuerpo(long userId, String orderId, String executionId,
                                              String symbol, String quantity, String price) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", symbol);
        cuerpo.put("side", "BUY");
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal(price));
        cuerpo.put("executedAt", EJECUTADO_EN.toString());
        return cuerpo;
    }

    private static HttpHeaders cabeceras(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
