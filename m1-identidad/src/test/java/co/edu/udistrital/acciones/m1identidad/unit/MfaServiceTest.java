package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfaServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private MfaService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new MfaService(redis);
    }

    @Test
    void generateAndStoreCreatesSixDigitCodeWithTtl() {
        Long userId = 42L;

        String code = service.generateAndStore(userId);

        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"), "Código debe ser 6 dígitos");

        verify(valueOps).set(eq("mfa:" + userId), eq(code), eq(Duration.ofMinutes(5)));
    }

    @Test
    void verifyReturnsTrueWhenCodeMatches() {
        Long userId = 42L;
        String code = "123456";

        when(valueOps.getAndDelete("mfa:" + userId)).thenReturn(code);

        boolean result = service.verify(userId, code);

        assertTrue(result);
    }

    @Test
    void verifyReturnsFalseWhenCodeDoesNotMatch() {
        Long userId = 42L;

        when(valueOps.getAndDelete("mfa:" + userId)).thenReturn("654321");

        boolean result = service.verify(userId, "123456");

        assertFalse(result);
    }

    @Test
    void verifyReturnsFalseWhenNoCodeStored() {
        Long userId = 42L;

        when(valueOps.getAndDelete("mfa:" + userId)).thenReturn(null);

        boolean result = service.verify(userId, "123456");

        assertFalse(result);
    }
}