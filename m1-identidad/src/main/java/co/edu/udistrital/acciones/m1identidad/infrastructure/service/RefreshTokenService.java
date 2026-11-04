package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenService {
    private final StringRedisTemplate redis;
    public RefreshTokenService(StringRedisTemplate redis) { this.redis = redis; }

    public void store(Long userId, String token) {
        redis.opsForValue().set("refresh:" + userId, token, Duration.ofDays(7));
    }

    public String get(Long userId) { return redis.opsForValue().get("refresh:" + userId); }

    public void revoke(Long userId) { redis.delete("refresh:" + userId); }
}
