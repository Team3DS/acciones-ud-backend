package co.edu.udistrital.acciones.m1identidad.integration;

import co.edu.udistrital.acciones.m1identidad.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class M1IdentidadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
        registry.add("CIFRADO_KEY", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("app.jwt.secret", () -> "test-jwt-secret-0123456789abcdef0123456789abcdef-for-integration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void fullAuthFlowRegisterLoginMfaVerifyLogout() {
        String correo = "integracion@test.com";
        String password = "password123";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> registerReq = Map.of(
                "nombres", "Integracion",
                "apellidos", "Test",
                "tipoDocumento", "CC",
                "numeroDocumento", "87654321",
                "correo", correo,
                "password", password,
                "nivelInversionista", "AVANZADO",
                "celular", "3001234567"
        );
        ResponseEntity<Map> registerResp = rest.postForEntity("/api/v1/auth/register", new HttpEntity<>(registerReq, headers), Map.class);
        assertEquals(HttpStatus.OK, registerResp.getStatusCode());
        assertNotNull(registerResp.getBody().get("id"));

        Map<String, Object> loginReq = Map.of("correo", correo, "password", password);
        ResponseEntity<Map> loginResp = rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(loginReq, headers), Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        String accessToken = (String) loginResp.getBody().get("accessToken");
        String refreshToken = (String) loginResp.getBody().get("refreshToken");
        String userId = String.valueOf(loginResp.getBody().get("userId"));
        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth(accessToken);

        String otp = redisTemplate.opsForValue().get("mfa:" + userId);
        assertNotNull(otp, "El OTP debe existir en Redis tras el login");
        Map<String, Object> mfaReq = Map.of("userId", Long.valueOf(userId), "code", otp);
        ResponseEntity<Map> mfaResp = rest.postForEntity("/api/v1/auth/mfa/verify", new HttpEntity<>(mfaReq, authHeaders), Map.class);
        assertEquals(HttpStatus.OK, mfaResp.getStatusCode());
        assertEquals(true, mfaResp.getBody().get("verified"));

        Map<String, Object> logoutReq = Map.of("userId", Long.valueOf(userId), "refreshToken", refreshToken);
        ResponseEntity<Map> logoutResp = rest.postForEntity("/api/v1/auth/logout", new HttpEntity<>(logoutReq, authHeaders), Map.class);
        assertEquals(HttpStatus.OK, logoutResp.getStatusCode());
    }
}