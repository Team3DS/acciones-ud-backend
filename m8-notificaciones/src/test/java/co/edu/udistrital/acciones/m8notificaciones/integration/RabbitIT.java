package co.edu.udistrital.acciones.m8notificaciones.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import co.edu.udistrital.acciones.m8notificaciones.infrastructure.config.RabbitConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

class RabbitIT extends IntegrationSupport {
    private static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:4.3.0-management-alpine");
    @Autowired RabbitTemplate rabbit;

    @DynamicPropertySource
    static void broker(DynamicPropertyRegistry registry) {
        RABBIT.start();
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.rabbitmq.listener.simple.auto-startup", () -> true);
        registry.add("management.health.rabbit.enabled", () -> true);
    }

    @BeforeEach
    void purge() {
        RabbitAdmin admin = new RabbitAdmin(rabbit.getConnectionFactory());
        for (String queue :
                new String[] {RabbitConfig.QUEUE, RabbitConfig.DLQ, RabbitConfig.RETRY_QUEUE}) {
            admin.purgeQueue(queue, false);
        }
    }

    @Test
    void entregaRealDeduplicaEventoYCorreo() throws Exception {
        Message message = mensaje(Map.of("userId", 1));
        publicar("acciones.ud", "m5.orden.creada", message);
        publicar("acciones.ud", "m5.orden.creada", message);
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(
                        () -> {
                            assertThat(count("inbox_evento")).isEqualTo(1);
                            assertThat(
                                            jdbc.queryForObject(
                                                    "SELECT procesado_en IS NOT NULL FROM"
                                                            + " inbox_evento",
                                                    Boolean.class))
                                    .isTrue();
                        });
        assertThat(count("notificacion")).isEqualTo(1);
        deliveries.procesar(10);
        HTTP.verify(1, postRequestedFor(urlEqualTo("/emails")));
    }

    @Test
    void enviaEventoInvalidoADlqSinEfectosParciales() throws Exception {
        publicar("acciones.ud", "m5.orden.creada", mensaje(Map.of("userId", -1)));
        AtomicReference<Message> rejected = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15))
                .until(
                        () -> {
                            rejected.set(rabbit.receive(RabbitConfig.DLQ));
                            return rejected.get() != null;
                        });
        assertThat(count("inbox_evento")).isZero();
        assertThat(count("notificacion")).isZero();
    }

    @Test
    void recuperaEventoDesdeColaDeReintentoConTtl() throws Exception {
        Message delayed =
                MessageBuilder.fromClonedMessage(mensaje(Map.of("userId", 1)))
                        .setHeader("m8-original-key", "m5.orden.creada")
                        .setHeader("m8-attempt", 1)
                        .build();
        publicar("acciones.ud.retry", "m8.retry", delayed);
        await().atMost(Duration.ofSeconds(25))
                .untilAsserted(() -> assertThat(count("notificacion")).isEqualTo(1));
    }

    private Message mensaje(Map<String, Object> payload) throws Exception {
        UUID id = UUID.randomUUID();
        return MessageBuilder.withBody(
                        mapper.writeValueAsBytes(
                                Map.of(
                                        "eventId",
                                        id.toString(),
                                        "eventType",
                                        "m5.orden.creada",
                                        "occurredAt",
                                        Instant.now().toString(),
                                        "payload",
                                        payload)))
                .setMessageId(id.toString())
                .setContentType("application/json")
                .setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT)
                .build();
    }

    private void publicar(String exchange, String key, Message message) throws Exception {
        CorrelationData confirmation = new CorrelationData(UUID.randomUUID().toString());
        rabbit.send(exchange, key, message, confirmation);
        assertThat(confirmation.getFuture().get(5, TimeUnit.SECONDS).isAck()).isTrue();
        assertThat(confirmation.getReturned()).isNull();
    }
}
