package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Separate purpose, keys and attempt budget from login MFA. Never log challenge values. */
@Service
public class PasswordResetTokenService {
    public static final Duration TTL = Duration.ofMinutes(5);
    public static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final DefaultRedisScript<Long> ISSUE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[3]) == 1 or redis.call('EXISTS', KEYS[4]) == 1 then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('DEL', KEYS[2])
            redis.call('SET', KEYS[3], '1', 'PX', ARGV[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CLAIM = new DefaultRedisScript<>("""
            local stored = redis.call('GET', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])
            if not stored or ttl <= 0 or redis.call('EXISTS', KEYS[4]) == 1 then
                return 0
            end
            if stored ~= ARGV[1] then
                local attempts = redis.call('INCR', KEYS[2])
                redis.call('PEXPIRE', KEYS[2], ttl)
                if attempts >= 5 then redis.call('DEL', KEYS[1], KEYS[2]) end
                return 0
            end
            redis.call('SET', KEYS[4], ARGV[2], 'PX', ttl)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[4]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> COMPLETE = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[4]) ~= ARGV[1] then return 0 end
            redis.call('DEL', KEYS[1], KEYS[2], KEYS[4])
            return 1
            """, Long.class);
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Optional<Challenge> issue(Long userId) {
        String code = String.format(java.util.Locale.ROOT, "%06d", random.nextInt(1_000_000));
        Instant expiresAt = Instant.now().plus(TTL);
        Long result = redis.execute(ISSUE, keys(userId), code,
                Long.toString(TTL.toMillis()), Long.toString(COOLDOWN.toMillis()));
        return Long.valueOf(1).equals(result) ? Optional.of(new Challenge(code, expiresAt)) : Optional.empty();
    }

    /** Holds the challenge until the surrounding database transaction commits or rolls back. */
    public Optional<String> claim(Long userId, String code) {
        if (userId == null || code == null) return Optional.empty();
        String token = UUID.randomUUID().toString();
        Long result = redis.execute(CLAIM, keys(userId), code, token);
        return Long.valueOf(1).equals(result) ? Optional.of(token) : Optional.empty();
    }

    public void release(Long userId, String token) {
        redis.execute(RELEASE, keys(userId), token);
    }

    public void complete(Long userId, String token) {
        redis.execute(COMPLETE, keys(userId), token);
    }

    private List<String> keys(Long userId) {
        String prefix = "password-reset:{" + userId + "}:";
        return List.of(prefix + "code", prefix + "attempts", prefix + "cooldown", prefix + "claim");
    }

    public record Challenge(String code, Instant expiresAt) {
        @Override
        public String toString() { return "Challenge[redacted]"; }
    }
}
