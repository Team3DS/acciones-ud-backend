package co.edu.udistrital.acciones.m8notificaciones.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.edu.udistrital.acciones.m8notificaciones.infrastructure.config.RabbitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Contrato M2 real por AMQP: validación, transacción, deduplicación, DLQ y reintento. */
@SpringBootTest
@ActiveProfiles("test")
class FondosRabbitContractIntegrationTest {
    private static final PostgreSQLContainer<?> DATABASE =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("avisos_fondos_contract")
                    .withUsername("test")
                    .withPassword("test");
    private static final RabbitMQContainer BROKER =
            new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        DATABASE.start();
        BROKER.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
        registry.add("spring.datasource.username", DATABASE::getUsername);
        registry.add("spring.datasource.password", DATABASE::getPassword);
        registry.add("spring.rabbitmq.host", BROKER::getHost);
        registry.add("spring.rabbitmq.port", BROKER::getAmqpPort);
        registry.add("spring.rabbitmq.username", BROKER::getAdminUsername);
        registry.add("spring.rabbitmq.password", BROKER::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> true);
        registry.add("app.jobs.enabled", () -> false);
        registry.add("app.monthly.enabled", () -> false);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private ObjectMapper mapper;

    @BeforeEach
    void resetFixture() {
        RabbitAdmin admin = new RabbitAdmin(rabbit.getConnectionFactory());
        for (String queue : new String[] {RabbitConfig.QUEUE, RabbitConfig.DLQ, RabbitConfig.RETRY_QUEUE}) {
            admin.purgeQueue(queue, true);
        }
        jdbc.execute("TRUNCATE notificacion, entrega_correo, inbox_evento RESTART IDENTITY");
    }

    @ParameterizedTest
    @CsvSource({
        "m2.fondos.confirmados, ACTUALIZACION_SALDO",
        "m2.fondos.liberados, LIBERACION_FONDOS"
    })
    void eventoConOwnerYOrdenCreaAvisoYEntregaPendienteUnaSolaVez(String type, String category)
            throws Exception {
        UUID event = UUID.randomUUID();
        Message message = message(event, type, payload(type, true));
        send(type, message);
        assertProcessed(event, category);

        send(type, message);
        await().atMost(Duration.ofSeconds(5)).during(Duration.ofMillis(300))
                .untilAsserted(() -> assertSingleNotification(event, category));
        assertThat(queueDepth(RabbitConfig.DLQ)).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "m2.fondos.confirmados, ACTUALIZACION_SALDO",
        "m2.fondos.liberados, LIBERACION_FONDOS"
    })
    void legacySinOwnerVaADlqYElMismoUuidCorregidoPuedeRecuperarse(String type, String category)
            throws Exception {
        UUID event = UUID.randomUUID();
        Map<String, Object> legacy = payload(type, false);
        legacy.remove("orderId");
        send(type, message(event, type, legacy));
        assertDeadWithoutEffects();

        // El owner y la orden se aportan expresamente desde evidencia; el consumidor no los inventa.
        Message recovered = message(event, type, payload(type, true));
        send(type, recovered);
        assertProcessed(event, category);
        send(type, recovered);
        await().atMost(Duration.ofSeconds(5)).during(Duration.ofMillis(300))
                .untilAsserted(() -> assertSingleNotification(event, category));
        assertThat(queueDepth(RabbitConfig.DLQ)).as("La evidencia legacy sigue en DLQ").isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"m2.fondos.confirmados", "m2.fondos.liberados"})
    void ownerCeroEsRechazadoSinAsignacionFicticiaNiEfectosParciales(String type) throws Exception {
        Map<String, Object> invalid = payload(type, true);
        invalid.put("userId", 0);
        send(type, message(UUID.randomUUID(), type, invalid));
        assertDeadWithoutEffects();
    }

    @Test
    void falloSqlTemporalRevierteInboxYReintentaElMismoUuidSinDuplicados() throws Exception {
        UUID event = UUID.randomUUID();
        String type = "m2.fondos.confirmados";
        // La secuencia no se revierte al fallar la transacción: solo el primer INSERT falla.
        jdbc.execute("CREATE SEQUENCE qa_m8_fail_once_sequence START 1");
        jdbc.execute("""
                CREATE FUNCTION qa_m8_fail_once() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF nextval('qa_m8_fail_once_sequence') = 1 THEN
                        RAISE EXCEPTION 'fixture transient serialization failure' USING ERRCODE = '40001';
                    END IF;
                    RETURN NEW;
                END;
                $$
                """);
        jdbc.execute("CREATE TRIGGER qa_m8_fail_once_trigger BEFORE INSERT ON inbox_evento "
                + "FOR EACH ROW EXECUTE FUNCTION qa_m8_fail_once()");
        try {
            Message original = message(event, type, payload(type, true));
            send(type, original);
            await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
                assertThat(queueDepth(RabbitConfig.RETRY_QUEUE)).isEqualTo(1);
                assertNoDatabaseEffects();
            });
            assertProcessed(event, "ACTUALIZACION_SALDO");
            assertThat(queueDepth(RabbitConfig.DLQ)).isZero();
            assertThat(queueDepth(RabbitConfig.RETRY_QUEUE)).isZero();
            send(type, original);
            await().atMost(Duration.ofSeconds(5)).during(Duration.ofMillis(300))
                    .untilAsserted(() -> assertSingleNotification(event, "ACTUALIZACION_SALDO"));
        } finally {
            jdbc.execute("DROP TRIGGER qa_m8_fail_once_trigger ON inbox_evento");
            jdbc.execute("DROP FUNCTION qa_m8_fail_once()");
            jdbc.execute("DROP SEQUENCE qa_m8_fail_once_sequence");
        }
    }

    private Map<String, Object> payload(String type, boolean includeOwner) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reservationId", 7);
        payload.put("orderId", "4");
        payload.put("amount", new BigDecimal("12.34"));
        if (includeOwner) payload.put("userId", 42);
        if (type.endsWith("confirmados")) payload.put("executionId", "fill-4");
        else payload.put("reason", "Cancelación QA de contrato");
        return payload;
    }

    private Message message(UUID event, String type, Map<String, Object> payload) throws Exception {
        return MessageBuilder.withBody(mapper.writeValueAsBytes(Map.of(
                        "eventId", event.toString(), "source", "M2", "eventType", type,
                        "occurredAt", Instant.now().toString(), "aggregateId", "7", "payload", payload)))
                .setMessageId(event.toString())
                .setContentType("application/json")
                .build();
    }

    private void send(String type, Message message) throws Exception {
        CorrelationData confirmation = new CorrelationData(UUID.randomUUID().toString());
        rabbit.send("acciones.ud", type, message, confirmation);
        assertThat(confirmation.getFuture().get(5, TimeUnit.SECONDS).isAck()).isTrue();
        assertThat(confirmation.getReturned()).isNull();
    }

    private void assertProcessed(UUID event, String category) {
        await().atMost(Duration.ofSeconds(25)).untilAsserted(() -> assertSingleNotification(event, category));
    }

    private void assertSingleNotification(UUID event, String category) {
        assertThat(count("inbox_evento")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT procesado_en IS NOT NULL FROM inbox_evento WHERE evento_id = ?",
                Boolean.class, event)).isTrue();
        assertThat(count("notificacion")).isEqualTo(1);
        Map<String, Object> notification = jdbc.queryForMap(
                "SELECT usuario_id,orden_id,tipo_evento,visible_app,evento_id FROM notificacion");
        assertThat(((Number) notification.get("usuario_id")).longValue()).isEqualTo(42);
        assertThat(((Number) notification.get("orden_id")).longValue()).isEqualTo(4);
        assertThat(notification.get("tipo_evento")).isEqualTo(category);
        assertThat(notification.get("visible_app")).isEqualTo(true);
        assertThat(notification.get("evento_id")).isEqualTo(event);
        assertThat(count("entrega_correo")).isEqualTo(1);
        Map<String, Object> delivery = jdbc.queryForMap("SELECT usuario_id,estado,intentos FROM entrega_correo");
        assertThat(((Number) delivery.get("usuario_id")).longValue()).isEqualTo(42);
        assertThat(delivery.get("estado")).isEqualTo("PENDIENTE");
        assertThat(((Number) delivery.get("intentos")).intValue()).isZero();
    }

    private void assertDeadWithoutEffects() {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(queueDepth(RabbitConfig.DLQ)).isEqualTo(1);
            assertNoDatabaseEffects();
        });
    }

    private void assertNoDatabaseEffects() {
        assertThat(count("inbox_evento")).isZero();
        assertThat(count("notificacion")).isZero();
        assertThat(count("entrega_correo")).isZero();
    }

    private int count(String table) {
        if (!java.util.Set.of("inbox_evento", "notificacion", "entrega_correo").contains(table)) {
            throw new IllegalArgumentException("Tabla de fixture no permitida");
        }
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private int queueDepth(String queue) {
        var properties = new RabbitAdmin(rabbit.getConnectionFactory()).getQueueProperties(queue);
        assertThat(properties).isNotNull();
        return ((Number) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).intValue();
    }
}
