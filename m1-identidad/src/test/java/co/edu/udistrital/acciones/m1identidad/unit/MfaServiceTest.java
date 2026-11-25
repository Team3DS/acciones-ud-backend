package co.edu.udistrital.acciones.m1identidad.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

class MfaServiceTest {
    private static final List<String> KEYS = List.of("mfa:42", "mfa:attempts:42");
    private StringRedisTemplate redis;
    private MfaService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        service = new MfaService(redis);
    }

    @Test
    void storesSixDigitCodeAndResetsAttemptsAtomicallyWithFiveMinuteTtl() {
        doReturn(1L)
                .when(redis)
                .execute(any(RedisScript.class), eq(KEYS), anyString(), eq("300000"));
        String code = service.generateAndStore(42L);
        assertThat(code).matches("[0-9]{6}");
        assertThat(MfaService.TTL).isEqualTo(Duration.ofMinutes(5));
        verify(redis).execute(any(RedisScript.class), eq(KEYS), eq(code), eq("300000"));
    }

    @Test
    void acceptsOnlyAtomicScriptSuccess() {
        doReturn(1L).when(redis).execute(any(RedisScript.class), eq(KEYS), eq("123456"), eq("5"));
        assertThat(service.verify(42L, "123456")).isTrue();
        verify(redis).execute(any(RedisScript.class), eq(KEYS), eq("123456"), eq("5"));
    }

    @Test
    void rejectsMismatchExpiredOrConsumedCode() {
        doReturn(0L).when(redis).execute(any(RedisScript.class), eq(KEYS), eq("123456"), eq("5"));
        assertThat(service.verify(42L, "123456")).isFalse();
    }

    @Test
    void rejectsMissingScriptResult() {
        assertThat(service.verify(42L, "123456")).isFalse();
    }

    @Test
    void doesNotReturnCodeWhenRedisDoesNotConfirmStorage() {
        assertThatThrownBy(() -> service.generateAndStore(42L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingInputDoesNotConsumeCode() {
        assertThat(service.verify(null, "123456")).isFalse();
        assertThat(service.verify(42L, null)).isFalse();
        verifyNoInteractions(redis);
    }
}
