package co.edu.udistrital.acciones.m8notificaciones.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import co.edu.udistrital.acciones.m8notificaciones.application.service.AlertasService;
import co.edu.udistrital.acciones.m8notificaciones.application.service.EntregasService;
import co.edu.udistrital.acciones.m8notificaciones.application.service.EventosService;
import co.edu.udistrital.acciones.m8notificaciones.application.service.ReportesService;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationSupport {
    static final String SECRET = "m8-test-only-jwt-secret-1234567890123456789012345678901234567890";
    static final WireMockServer HTTP = new WireMockServer(options().dynamicPort());
    private static PostgreSQLContainer<?> database;

    static {
        HTTP.start();
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired EventosService events;
    @Autowired EntregasService deliveries;
    @Autowired AlertasService alerts;
    @Autowired ReportesService reports;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String external = System.getProperty("m8.test.jdbc-url");
        if (external == null || external.isBlank()) {
            if (database == null) {
                database = new PostgreSQLContainer<>("postgres:16.4-alpine");
                database.start();
            }
            registry.add("spring.datasource.url", database::getJdbcUrl);
            registry.add("spring.datasource.username", database::getUsername);
            registry.add("spring.datasource.password", database::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> external);
            registry.add(
                    "spring.datasource.username",
                    () -> System.getProperty("m8.test.username", "postgres"));
            registry.add(
                    "spring.datasource.password", () -> System.getProperty("m8.test.password", ""));
        }
        registry.add("app.resend.base-url", HTTP::baseUrl);
        for (String service : new String[] {"m1", "m3", "m4", "m5", "m6", "m10"}) {
            registry.add("app.services." + service, HTTP::baseUrl);
        }
    }

    @BeforeEach
    void reset() {
        jdbc.execute(
                "TRUNCATE"
                    + " notificacion,preferencia_notificacion,suscripcion_aviso_mercado,alerta_precio,reporte_mensual,inbox_evento,entrega_correo"
                    + " RESTART IDENTITY");
        HTTP.resetAll();
        HTTP.stubFor(
                post(urlEqualTo("/emails"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"id\":\"provider-fixture\"}")));
        HTTP.stubFor(
                get(urlPathMatching("/internal/users/[0-9]+/notification-contact"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"email\":\"fixture@example.test\"}")));
        entitlement(true);
        HTTP.stubFor(
                get(urlEqualTo("/api/v1/instruments/id/1"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"id\":1,\"symbol\":\"AAPL\",\"enabled\":true}")));
        quote("151", Instant.now());
        HTTP.stubFor(
                get(urlPathMatching("/internal/reports/monthly/[0-9]+"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"grossProfit\":12.5,\"commissions\":0.8,\"currency\":\"USD\"}")));
        HTTP.stubFor(
                get(urlPathMatching("/internal/portfolio/[0-9]+/summary"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"assetsSummary\":\"AAPL: 2 acciones\"}")));
        HTTP.stubFor(
                get(urlEqualTo("/internal/users/monthly-report-subscribers?page=0&size=100"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"content\":[{\"userId\":7,\"email\":\"report@example.test\"}]}")));
        HTTP.stubFor(
                get(urlEqualTo("/internal/users/monthly-report-subscribers?page=1&size=100"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"content\":[]}")));
    }

    void entitlement(boolean active) {
        HTTP.stubFor(
                get(urlPathMatching("/internal/subscriptions/[0-9]+/entitlement"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"active\":"
                                                        + active
                                                        + ",\"validUntil\":\""
                                                        + Instant.now().plusSeconds(3600)
                                                        + "\"}")));
    }

    void quote(String price, Instant timestamp) {
        HTTP.stubFor(
                get(urlEqualTo("/api/v1/quotes/AAPL"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"symbol\":\"AAPL\",\"price\":"
                                                        + price
                                                        + ",\"timestamp\":\""
                                                        + timestamp
                                                        + "\"}")));
    }

    EventoAviso event(String type, Map<String, Object> payload) {
        return new EventoAviso(
                UUID.randomUUID(),
                type.substring(0, type.indexOf('.')).toUpperCase(java.util.Locale.ROOT),
                type,
                Instant.now(),
                mapper.valueToTree(payload));
    }

    String token(long user) {
        return "Bearer "
                + Jwts.builder()
                        .subject(Long.toString(user))
                        .expiration(Date.from(Instant.now().plusSeconds(600)))
                        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                        .compact();
    }

    int count(String table) {
        if (!java.util.Set.of(
                        "notificacion",
                        "inbox_evento",
                        "entrega_correo",
                        "reporte_mensual",
                        "alerta_precio")
                .contains(table)) {
            throw new IllegalArgumentException();
        }
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }
}
