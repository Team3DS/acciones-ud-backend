package co.edu.udistrital.acciones.m3catalogomercados.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoRepositoryPort;
import co.edu.udistrital.acciones.m3catalogomercados.domain.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class CatalogoIntegrationSupport {
    private static PostgreSQLContainer<?> postgres;
    private static org.testcontainers.containers.RabbitMQContainer rabbit;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.web.context.WebApplicationContext context;

    @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}")
    String jwtSecret;

    String token(java.time.Instant expiration, String secret) {
        return io.jsonwebtoken.Jwts.builder()
                .subject("1")
                .expiration(java.util.Date.from(expiration))
                .signWith(
                        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
    }

    @BeforeEach
    void authenticate() {
        mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(
                                context)
                        .addFilters(
                                context.getBean(
                                        co.edu.udistrital.acciones.m3catalogomercados.infrastructure
                                                .config.CorrelationIdFilter.class))
                        .apply(
                                org.springframework.security.test.web.servlet.setup
                                        .SecurityMockMvcConfigurers.springSecurity())
                        .defaultRequest(
                                get("/").header(
                                                "Authorization",
                                                "Bearer "
                                                        + token(
                                                                java.time.Instant.now()
                                                                        .plusSeconds(600),
                                                                jwtSecret)))
                        .build();
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogoRepositoryPort repository;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (Boolean.getBoolean("m3.test.rabbit")) {
            if (rabbit == null) {
                rabbit =
                        new org.testcontainers.containers.RabbitMQContainer(
                                "rabbitmq:4-management-alpine");
                rabbit.start();
            }
            registry.add("spring.rabbitmq.host", rabbit::getHost);
            registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
            registry.add("management.health.rabbit.enabled", () -> true);
        }
        String external = System.getProperty("m3.test.jdbc-url");
        if (external != null && !external.isBlank()) {
            registry.add("spring.datasource.url", () -> external);
            registry.add(
                    "spring.datasource.username",
                    () -> System.getProperty("m3.test.username", "postgres"));
            registry.add(
                    "spring.datasource.password", () -> System.getProperty("m3.test.password", ""));
        } else {
            postgres =
                    new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("catalogo_db");
            postgres.start();
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
