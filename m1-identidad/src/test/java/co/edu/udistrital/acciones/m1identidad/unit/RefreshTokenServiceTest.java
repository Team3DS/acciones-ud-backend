package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new RefreshTokenService(redis);
    }

    @Test
    void storeSavesTokenWithSevenDayTtl() {
        Long userId = 42L;
        String token = "refresh-token-abc";

        service.store(userId, token);

        verify(valueOps).set(eq("refresh:" + userId), eq(token), eq(Duration.ofDays(7)));
    }

    @Test
    void getReturnsStoredToken() {
        Long userId = 42L;
        String token = "refresh-token-xyz";

        when(valueOps.get("refresh:" + userId)).thenReturn(token);

        String result = service.get(userId);

        assertEquals(token, result);
    }

    @Test
    void getReturnsNullWhenNotFound() {
        Long userId = 42L;

        when(valueOps.get("refresh:" + userId)).thenReturn(null);

        String result = service.get(userId);

        assertNull(result);
    }

    @Test
    void revokeDeletesKey() {
        Long userId = 42L;

        service.revoke(userId);

        verify(redis).delete("refresh:" + userId);
    }
}