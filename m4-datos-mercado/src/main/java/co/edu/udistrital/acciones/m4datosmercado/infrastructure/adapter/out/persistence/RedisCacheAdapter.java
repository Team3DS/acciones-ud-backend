package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisCacheAdapter implements CacheCotizacionesPort {

    private static final String KEY_PREFIX = "quote:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Cotizacion> get(String symbol) {
        String key = KEY_PREFIX + symbol;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, Cotizacion.class));
        } catch (Exception ex) {
            redisTemplate.delete(key);
            return Optional.empty();
        }
    }

    @Override
    public void put(String symbol, Cotizacion cotizacion, Duration ttl) {
        String key = KEY_PREFIX + symbol;
        try {
            String json = objectMapper.writeValueAsString(cotizacion);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception ex) {
            throw new RuntimeException("Error serializando cotización para cache", ex);
        }
    }

    @Override
    public void evict(String symbol) {
        redisTemplate.delete(KEY_PREFIX + symbol);
    }
}