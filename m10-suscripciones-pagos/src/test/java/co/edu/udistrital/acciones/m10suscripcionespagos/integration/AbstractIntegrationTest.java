package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("pagos_db")
            .withUsername("test")
            .withPassword("test");

    private static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    protected static final WireMockServer STRIPE = new WireMockServer(options().dynamicPort());
    protected static final WireMockServer SERVICIOS = new WireMockServer(options().dynamicPort());

    static {
        POSTGRES.start();
        RABBITMQ.start();
        STRIPE.start();
        SERVICIOS.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("app.jwt.secret", () -> TokenTestFactory.SECRETO);
        registry.add("app.internal.token", () -> TokenTestFactory.TOKEN_INTERNO);
        registry.add("app.stripe.api-base", STRIPE::baseUrl);
        registry.add("app.stripe.api-key", () -> "sk_test_integracion");
        registry.add("app.stripe.webhook-secret", () -> "whsec_integracion");
        registry.add("app.webhooks.procesador-enabled", () -> "false");
        registry.add("app.clientes-http.m3-base-url", SERVICIOS::baseUrl);
        registry.add("app.clientes-http.m4-base-url", SERVICIOS::baseUrl);
    }

    @Autowired
    protected TestRestTemplate rest;
}
