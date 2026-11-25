package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;

@Service
public class MfaService {
    private static final int TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    /** Vigencia del código MFA. Debe ser menor a 15 minutos, el máximo que acepta M8. */
    public static final Duration TTL = Duration.ofMinutes(TTL_MINUTES);

    private static final String KEY_PREFIX = "mfa:";
    private static final String ATTEMPTS_KEY_PREFIX = "mfa:attempts:";
    private static final DefaultRedisScript<Long> STORE_CODE =
            new DefaultRedisScript<>(
                    """
                    redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
                    redis.call('DEL', KEYS[2])
                    return 1
                    """,
                    Long.class);
    private static final DefaultRedisScript<Long> VERIFY_CODE =
            new DefaultRedisScript<>(
                    """
                    local stored = redis.call('GET', KEYS[1])
                    if not stored then
                        redis.call('DEL', KEYS[2])
                        return 0
                    end
                    local ttl = redis.call('PTTL', KEYS[1])
                    if ttl <= 0 then
                        redis.call('DEL', KEYS[1], KEYS[2])
                        return 0
                    end
                    local attempts = redis.call('INCR', KEYS[2])
                    redis.call('PEXPIRE', KEYS[2], ttl)
                    local limit = tonumber(ARGV[2])
                    if attempts > limit then
                        redis.call('DEL', KEYS[1], KEYS[2])
                        return 0
                    end
                    if stored == ARGV[1] then
                        redis.call('DEL', KEYS[1], KEYS[2])
                        return 1
                    end
                    if attempts >= limit then
                        redis.call('DEL', KEYS[1], KEYS[2])
                    end
                    return 0
                    """,
                    Long.class);

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public MfaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String generateAndStore(Long userId) {
        String code = String.format("%06d", random.nextInt(1_000_000));
        Long stored = redis.execute(STORE_CODE, keys(userId), code, Long.toString(TTL.toMillis()));
        if (!Long.valueOf(1).equals(stored)) {
            throw new IllegalStateException("No se pudo almacenar el código MFA");
        }
        return code;
    }

    public boolean verify(Long userId, String code) {
        if (userId == null || code == null) {
            return false;
        }
        Long accepted =
                redis.execute(VERIFY_CODE, keys(userId), code, Integer.toString(MAX_ATTEMPTS));
        return Long.valueOf(1).equals(accepted);
    }

    public void revoke(Long userId) {
        redis.delete(keys(userId));
    }

    private List<String> keys(Long userId) {
        return List.of(KEY_PREFIX + userId, ATTEMPTS_KEY_PREFIX + userId);
    }
}
