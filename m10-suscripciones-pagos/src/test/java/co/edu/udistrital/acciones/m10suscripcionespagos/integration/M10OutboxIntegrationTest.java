package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class M10OutboxIntegrationTest extends AbstractIntegrationTest {

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

    @Autowired
    private OutboxPort outbox;

    @Autowired
    private TransactionTemplate tx;

    @Test
    void flywayAplicaLaTablaDeOutbox() {
        Integer tablas = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'outbox_evento'
                """, Integer.class);
        Integer versiones = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '4' AND success = true",
                Integer.class);

        assertThat(tablas).isEqualTo(1);
        assertThat(versiones).isEqualTo(1);
    }

    @Test
    void elEventoRegistradoEnElOutboxLlegaConElSobreCompletoYQuedaPublicado() throws Exception {
        String cola = "m10.prueba." + UUID.randomUUID();
        Queue destino = new Queue(cola, false, false, true);
        amqpAdmin.declareQueue(destino);
        amqpAdmin.declareBinding(BindingBuilder.bind(destino).to(accionesExchange).with("m10.#"));
        String correlacion = "corr-outbox-m10";
        try {
            MDC.put(CorrelationIdFilter.CLAVE_MDC, correlacion);
            SuscripcionActivadaPayload payload = new SuscripcionActivadaPayload(
                    7701L, "PREMIUM", "MENSUAL", "sub_outbox_m10",
                    Instant.parse("2027-01-01T00:00:00Z"));
            tx.executeWithoutResult(estado -> outbox.registrar(
                    RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "7701",
                    RoutingKeys.SUSCRIPCION_ACTIVADA, payload));
            MDC.remove(CorrelationIdFilter.CLAVE_MDC);

            Message mensaje = rabbitTemplate.receive(cola, 20_000);
            assertThat(mensaje).isNotNull();
            assertThat(mensaje.getMessageProperties().getReceivedRoutingKey())
                    .isEqualTo(RoutingKeys.SUSCRIPCION_ACTIVADA);
            assertThat(mensaje.getMessageProperties().getContentType())
                    .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
            assertThat(mensaje.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
            assertThat((String) mensaje.getMessageProperties().getHeader(CorrelationIdFilter.HEADER))
                    .isEqualTo(correlacion);

            JsonNode sobre = objectMapper.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            assertThat(sobre.get("eventId").asText())
                    .isEqualTo(mensaje.getMessageProperties().getMessageId());
            assertThat(sobre.get("eventType").asText()).isEqualTo(RoutingKeys.SUSCRIPCION_ACTIVADA);
            assertThat(sobre.get("source").asText()).isEqualTo("M10");
            assertThat(sobre.get("aggregateId").asText()).isEqualTo("7701");
            assertThat(sobre.get("correlationId").asText()).isEqualTo(correlacion);
            assertThat(sobre.get("occurredAt").asText()).isNotBlank();

            JsonNode cuerpo = sobre.get("payload");
            assertThat(cuerpo.get("userId").asLong()).isEqualTo(7701L);
            assertThat(cuerpo.get("planId").asText()).isEqualTo("PREMIUM");
            assertThat(cuerpo.get("interval").asText()).isEqualTo("MENSUAL");
            assertThat(cuerpo.get("stripeSubscriptionId").asText()).isEqualTo("sub_outbox_m10");

            await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> assertThat(publicado("sub_outbox_m10")).isTrue());
            Integer reintentos = jdbc.queryForObject(
                    "SELECT reintentos FROM outbox_evento WHERE payload LIKE ?", Integer.class,
                    "%sub_outbox_m10%");
            assertThat(reintentos).isZero();
        } finally {
            MDC.remove(CorrelationIdFilter.CLAVE_MDC);
            amqpAdmin.deleteQueue(cola);
            jdbc.update("DELETE FROM outbox_evento WHERE payload LIKE ?", "%sub_outbox_m10%");
        }
    }

    private boolean publicado(String marca) {
        Boolean publicado = jdbc.queryForObject(
                "SELECT publicado FROM outbox_evento WHERE payload LIKE ?", Boolean.class, "%" + marca + "%");
        return Boolean.TRUE.equals(publicado);
    }
}
