package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtProperties props;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret("test-secret-key-for-testing-purposes-only-32-chars-minimum");
        props.setAccessTtlSeconds(900);
        jwtService = new JwtService(props);
    }

    @Test
    void generateAccessTokenProducesValidJwtToken() {
        String token = jwtService.generateAccessToken(100L);

        assertNotNull(token);
        assertTrue(token.length() > 0);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts (header.payload.signature)");

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertTrue(payload.contains("\"sub\":\"100\""), "Payload should contain userId as subject");
    }

    @Test
    void tokenContainsUserIdInPayload() {
        String token = jwtService.generateAccessToken(999L);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertTrue(payload.contains("\"sub\":\"999\""), "Payload should contain userId as subject");
    }

    @Test
    void parseUserIdExtractsUserIdFromToken() {
        String token = jwtService.generateAccessToken(42L);

        Long userId = jwtService.parseUserId(token);

        assertEquals(42L, userId);
    }
}