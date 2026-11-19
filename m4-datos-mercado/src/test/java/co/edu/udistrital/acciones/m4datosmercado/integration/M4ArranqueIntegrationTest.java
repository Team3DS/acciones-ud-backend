package co.edu.udistrital.acciones.m4datosmercado.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class M4ArranqueIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("mercado_db")
            .withUsername("acciones_ud")
            .withPassword("acciones_ud");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management")
            .withEnv("RABBITMQ_DEFAULT_USER", "acciones_ud")
            .withEnv("RABBITMQ_DEFAULT_PASS", "acciones_ud");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "acciones_ud");
        registry.add("spring.rabbitmq.password", () -> "acciones_ud");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("app.alpaca.api-key", () -> "test-key");
        registry.add("app.alpaca.api-secret", () -> "test-secret");
        registry.add("app.alpaca.base-url", () -> "http://localhost:8080");
        registry.add("app.alpaca.data-feed", () -> "dummy-feed");
        registry.add("app.m3.base-url", () -> "http://localhost:8083");
        registry.add("app.m3.timeout-ms", () -> "5000");
        registry.add("app.websocket.endpoint", () -> "/market/ws");
        registry.add("app.websocket.heartbeat-interval", () -> "20000");
        registry.add("app.jwt.secret", () -> "clave-secreta-para-testing-minimo-32-chars");
    }

    @Test
    void contextLoads() {
    }
}