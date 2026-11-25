package co.edu.udistrital.acciones.m1identidad.integration;

import co.edu.udistrital.acciones.m1identidad.Application;
import co.edu.udistrital.acciones.m1identidad.application.dto.PasswordResetConfirmRequest;
import co.edu.udistrital.acciones.m1identidad.application.dto.LoginRequest;
import co.edu.udistrital.acciones.m1identidad.application.port.out.UserRepositoryPort;
import co.edu.udistrital.acciones.m1identidad.application.service.PasswordResetApplicationService;
import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.RabbitConfig;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out.PasswordResetOutboxDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/** Isolated PostgreSQL + Redis + Rabbit; all accounts and codes are synthetic fixtures. */
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PasswordResetFlowIntegrationTest {
    private static final String OLD_PASSWORD = "fixture-old-password";
    private static final String NEW_PASSWORD = "  fixture-new-password  ";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("reset_test").withUsername("test").withPassword("test");
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");
    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("CIFRADO_KEY", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("app.jwt.secret", () -> "test-jwt-secret-0123456789abcdef0123456789abcdef-for-integration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.password-reset.outbox.enabled", () -> false);
    }

    @Autowired TestRestTemplate http;
    @Autowired StringRedisTemplate redis;
    @Autowired RabbitTemplate broker;
    @Autowired AmqpAdmin admin;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate sql;
    @Autowired PasswordResetApplicationService reset;
    @Autowired UserApplicationService auth;
    @Autowired UserRepositoryPort users;
    @Autowired PasswordResetOutboxDispatcher dispatcher;
    @SpyBean PasswordEncoder encoder;

    @BeforeEach
    void useHttpTransportThatReturns401WithoutRetryingPostBodies() {
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void requestIsGenericPublishesM8EnvelopeAndCooldownPreventsAnotherEvent() throws Exception {
        Fixture fixture = register();
        Queue queue = new Queue("reset-fixture-" + UUID.randomUUID(), false, false, false);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(RabbitConfig.EVENTS_EXCHANGE))
                .with("m1.recuperacion.solicitada"));
        try {
            var known = request(fixture.email());
            var unknown = request("missing-" + UUID.randomUUID() + "@example.test");
            assertEquals(HttpStatus.ACCEPTED, known.getStatusCode());
            assertEquals(known.getStatusCode(), unknown.getStatusCode());
            assertEquals(known.getBody(), unknown.getBody());
            assertEquals(1, known.getBody().size());
            assertTrue(known.getBody().containsKey("message"));

            Message message = broker.receive(queue.getName(), 5000);
            assertNotNull(message);
            JsonNode event = json.readTree(message.getBody());
            assertEquals("m1.recuperacion.solicitada", event.path("eventType").asText());
            assertEquals("M1", event.path("source").asText());
            assertDoesNotThrow(() -> UUID.fromString(event.path("eventId").asText()));
            assertEquals(event.path("eventId").asText(), message.getMessageProperties().getMessageId());
            JsonNode payload = event.path("payload");
            assertEquals(fixture.id(), payload.path("userId").asLong());
            assertEquals(fixture.email(), payload.path("email").asText());
            assertEquals(code(fixture), payload.path("code").asText());
            assertThat(Instant.parse(payload.path("expiresAt").asText()))
                    .isBetween(Instant.now().plusSeconds(280), Instant.now().plusSeconds(301));
            String original = code(fixture);
            assertEquals(known.getBody(), request(fixture.email()).getBody());
            assertEquals(original, code(fixture));
            assertNull(broker.receive(queue.getName(), 200));
        } finally { admin.deleteQueue(queue.getName()); }
    }

    @Test
    void correctResetRevokesRefreshAndPendingMfaAndStillRequiresNormalLoginMfa() {
        Fixture fixture = register();
        login(fixture, OLD_PASSWORD);
        String loginCode = redis.opsForValue().get("mfa:" + fixture.id());
        var session = post("/mfa/verify", Map.of("userId", fixture.id(), "code", loginCode));
        String oldRefresh = (String) session.getBody().get("refreshToken");
        assertNotNull(oldRefresh);
        assertEquals(HttpStatus.OK, post("/refresh", Map.of("userId", fixture.id(), "refreshToken", oldRefresh)).getStatusCode());
        login(fixture, OLD_PASSWORD);
        String pendingCode = redis.opsForValue().get("mfa:" + fixture.id());
        String previousState = sql.queryForObject("select estado_usuario from usuario where id=?", String.class, fixture.id());
        request(fixture.email());
        String resetCode = code(fixture);
        assertEquals(HttpStatus.UNAUTHORIZED, confirm(fixture, incorrect(resetCode), NEW_PASSWORD).getStatusCode());
        var success = confirm(fixture, resetCode, NEW_PASSWORD);
        assertEquals(HttpStatus.OK, success.getStatusCode());
        assertThat(success.getBody()).containsOnlyKeys("message");
        assertEquals(previousState, sql.queryForObject("select estado_usuario from usuario where id=?", String.class, fixture.id()));
        assertEquals(false, post("/mfa/verify", Map.of("userId", fixture.id(), "code", pendingCode)).getBody().get("verified"));
        assertEquals(HttpStatus.UNAUTHORIZED, post("/refresh", Map.of("userId", fixture.id(), "refreshToken", oldRefresh)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, login(fixture, OLD_PASSWORD).getStatusCode());
        var challenge = login(fixture, NEW_PASSWORD);
        assertEquals(HttpStatus.OK, challenge.getStatusCode());
        assertThat(challenge.getBody()).doesNotContainKeys("accessToken", "refreshToken");
        var newSession = post("/mfa/verify", Map.of("userId", fixture.id(), "code",
                redis.opsForValue().get("mfa:" + fixture.id())));
        assertEquals(true, newSession.getBody().get("verified"));
        assertNotNull(newSession.getBody().get("accessToken"));
        assertEquals(HttpStatus.UNAUTHORIZED, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
        assertEquals(1, completionCount(fixture));
    }

    @Test
    void passwordValidationDoesNotConsumeCodeAndUnknownAccountHasSame401AsWrongCode() {
        Fixture fixture = register();
        request(fixture.email());
        String resetCode = code(fixture);
        assertEquals(HttpStatus.BAD_REQUEST, confirm(fixture, resetCode, "short").getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, confirm(fixture, resetCode, "á".repeat(37)).getStatusCode());
        assertEquals(resetCode, code(fixture));
        assertFalse(Boolean.TRUE.equals(redis.hasKey(key(fixture, "attempts"))));
        var wrong = confirm(fixture, incorrect(resetCode), NEW_PASSWORD);
        var unknown = post("/password-reset/confirm", Map.of("correo", "missing@example.test", "code", resetCode,
                "newPassword", NEW_PASSWORD));
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.getStatusCode());
        assertEquals(wrong.getStatusCode(), unknown.getStatusCode());
        assertEquals(wrong.getBody(), unknown.getBody());
        assertEquals(HttpStatus.OK, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
    }

    @Test
    void databaseRollbackReleasesClaimWithoutConsumingCodeOrChangingPassword() {
        Fixture fixture = register();
        request(fixture.email());
        String resetCode = code(fixture);
        sql.execute("""
                create function fail_reset_fixture() returns trigger language plpgsql as $$
                begin raise exception 'fixture write conflict' using errcode='40001'; end $$
                """);
        sql.execute("create trigger fail_reset_fixture before update on usuario for each row execute function fail_reset_fixture()");
        try {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
            assertEquals(resetCode, code(fixture));
            assertFalse(Boolean.TRUE.equals(redis.hasKey(key(fixture, "claim"))));
            assertEquals(0, completionCount(fixture));
        } finally {
            sql.execute("drop trigger fail_reset_fixture on usuario");
            sql.execute("drop function fail_reset_fixture()");
        }
        assertEquals(HttpStatus.OK, login(fixture, OLD_PASSWORD).getStatusCode());
        assertEquals(HttpStatus.OK, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
        assertEquals(1, completionCount(fixture));
    }

    @Test
    void postgresLockMakesPendingMfaAndOldPasswordLoginWaitForResetThenRejectsThem() throws Exception {
        Fixture fixture = register();
        login(fixture, OLD_PASSWORD);
        String pendingMfa = redis.opsForValue().get("mfa:" + fixture.id());
        request(fixture.email());
        String resetCode = code(fixture);
        CountDownLatch encoding = new CountDownLatch(1);
        CountDownLatch finishEncoding = new CountDownLatch(1);
        doAnswer(invocation -> {
            if (NEW_PASSWORD.contentEquals(invocation.getArgument(0, CharSequence.class))) {
                encoding.countDown();
                assertTrue(finishEncoding.await(10, TimeUnit.SECONDS));
            }
            return invocation.callRealMethod();
        }).when(encoder).encode(any(CharSequence.class));
        var executor = Executors.newFixedThreadPool(3);
        try {
            var resetFuture = executor.submit(() -> reset.confirm(new PasswordResetConfirmRequest(
                    fixture.email(), resetCode, NEW_PASSWORD)));
            assertTrue(encoding.await(10, TimeUnit.SECONDS));
            var verifyFuture = executor.submit(() -> auth.verifyMfa(fixture.id(), pendingMfa));
            var loginFuture = executor.submit(() -> {
                try { auth.login(new LoginRequest(fixture.email(), OLD_PASSWORD)); return true; }
                catch (IllegalArgumentException rejected) { return false; }
            });
            // PostgreSQL actually observes the concurrent FOR UPDATE waiting for the held row lock.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            boolean waiting = false;
            while (System.nanoTime() < deadline) {
                Integer count = sql.queryForObject("select count(*) from pg_stat_activity where datname=current_database() and wait_event_type='Lock'", Integer.class);
                if (count != null && count >= 2) { waiting = true; break; }
                Thread.sleep(20);
            }
            assertTrue(waiting, "Both authentication paths must wait for the actual PostgreSQL row lock");
            assertFalse(verifyFuture.isDone());
            assertFalse(loginFuture.isDone());
            finishEncoding.countDown();
            resetFuture.get(10, TimeUnit.SECONDS);
            assertEquals(false, verifyFuture.get(10, TimeUnit.SECONDS).get("verified"));
            assertFalse(loginFuture.get(10, TimeUnit.SECONDS));
            assertNull(redis.opsForValue().get("refresh:" + fixture.id()));
        } finally {
            finishEncoding.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void twoHttpConfirmationsHaveExactlyOneSuccess() throws Exception {
        Fixture fixture = register();
        request(fixture.email());
        String resetCode = code(fixture);
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> { start.await(); return confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode(); });
            var second = executor.submit(() -> { start.await(); return confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode(); });
            start.countDown();
            assertThat(java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
            assertEquals(1, completionCount(fixture));
        } finally { start.countDown(); executor.shutdownNow(); }
    }

    @Test
    void completionOutboxRetainsUnroutableEventAndRetriesSameUuidAfterBindingExists() throws Exception {
        Fixture fixture = register();
        request(fixture.email());
        String resetCode = code(fixture);
        assertEquals(HttpStatus.OK, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
        UUID eventId = sql.queryForObject("select id from outbox_evento where tipo_evento='m1.recuperacion.completada' and agregado_id=?",
                UUID.class, Long.toString(fixture.id()));
        // The exchange exists, but no completion queue exists yet: ACK + mandatory RETURN is not delivery.
        dispatcher.publishPending();
        assertNull(sql.queryForObject("select publicado_en from outbox_evento where id=?", java.sql.Timestamp.class, eventId));
        assertThat(sql.queryForObject("select intentos from outbox_evento where id=?", Integer.class, eventId)).isGreaterThan(0);
        assertEquals("BROKER_UNCONFIRMED", sql.queryForObject("select ultimo_error from outbox_evento where id=?", String.class, eventId));

        Queue queue = new Queue("completion-fixture-" + UUID.randomUUID(), false, false, false);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(RabbitConfig.EVENTS_EXCHANGE))
                .with("m1.recuperacion.completada"));
        try {
            assertThat(dispatcher.publishPending()).isGreaterThan(0);
            assertNotNull(sql.queryForObject("select publicado_en from outbox_evento where id=?", java.sql.Timestamp.class, eventId));
            int matching = 0;
            Message message;
            while ((message = broker.receive(queue.getName())) != null) {
                if (!eventId.toString().equals(message.getMessageProperties().getMessageId())) continue;
                matching++;
                JsonNode event = json.readTree(message.getBody());
                assertEquals(eventId.toString(), event.path("eventId").asText());
                assertEquals("m1.recuperacion.completada", event.path("eventType").asText());
                assertEquals("M1", event.path("source").asText());
                assertEquals(2, event.path("payload").size());
                assertEquals(fixture.id(), event.path("payload").path("userId").asLong());
                assertEquals(fixture.email(), event.path("payload").path("email").asText());
                assertFalse(event.path("payload").has("code"));
                assertFalse(event.path("payload").has("newPassword"));
            }
            assertEquals(1, matching);
            assertEquals(HttpStatus.UNAUTHORIZED, confirm(fixture, resetCode, NEW_PASSWORD).getStatusCode());
            assertEquals(1, completionCount(fixture));
            assertEquals(0, dispatcher.publishPending());
            assertNull(broker.receive(queue.getName()));
        } finally { admin.deleteQueue(queue.getName()); }
    }

    private Fixture register() {
        String unique = UUID.randomUUID().toString();
        String email = unique + "@example.test";
        var response = post("/register", Map.of("nombres", "Fixture", "apellidos", "Recovery", "tipoDocumento", "CC",
                "numeroDocumento", unique, "correo", email, "password", OLD_PASSWORD,
                "nivelInversionista", "PRINCIPIANTE", "telefono", "3000000000"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return new Fixture(((Number) response.getBody().get("id")).longValue(), email);
    }

    private ResponseEntity<Map> request(String email) { return post("/password-reset/request", Map.of("correo", email)); }
    private ResponseEntity<Map> confirm(Fixture fixture, String code, String password) {
        return post("/password-reset/confirm", Map.of("correo", fixture.email(), "code", code, "newPassword", password));
    }
    private ResponseEntity<Map> login(Fixture fixture, String password) {
        return post("/login", Map.of("correo", fixture.email(), "password", password));
    }
    private ResponseEntity<Map> post(String path, Object body) {
        return http.postForEntity("/api/v1/auth" + path, body, Map.class);
    }
    private String code(Fixture fixture) { return redis.opsForValue().get(key(fixture, "code")); }
    private String key(Fixture fixture, String suffix) { return "password-reset:{" + fixture.id() + "}:" + suffix; }
    private String incorrect(String code) { return "000000".equals(code) ? "111111" : "000000"; }
    private int completionCount(Fixture fixture) {
        return sql.queryForObject("select count(*) from outbox_evento where tipo_evento='m1.recuperacion.completada' and agregado_id=?",
                Integer.class, Long.toString(fixture.id()));
    }
    private record Fixture(long id, String email) {}
}
