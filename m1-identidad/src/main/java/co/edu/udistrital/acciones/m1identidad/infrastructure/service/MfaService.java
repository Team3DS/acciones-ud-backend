package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class MfaService {
    private static final int OTP_LENGTH = 6;
    private static final int TTL_MINUTES = 5;
    private static final String KEY_PREFIX = "mfa:";

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public MfaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String generateAndStore(Long userId) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(KEY_PREFIX + userId, code, Duration.ofMinutes(TTL_MINUTES));
        return code;
    }

    public boolean verify(Long userId, String code) {
        String key = KEY_PREFIX + userId;
        String stored = redis.opsForValue().getAndDelete(key);
        return stored != null && stored.equals(code);
    }
}