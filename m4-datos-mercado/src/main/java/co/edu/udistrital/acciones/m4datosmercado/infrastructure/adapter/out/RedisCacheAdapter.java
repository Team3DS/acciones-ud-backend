package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisCacheAdapter implements CacheCotizacionesPort {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public RedisCacheAdapter(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Optional<Cotizacion> get(String symbol) {
        String key = "quote:" + symbol;
        String json = redis.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(mapper.readValue(json, Cotizacion.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String symbol, Cotizacion cotizacion, Duration ttl) {
        try {
            String json = mapper.writeValueAsString(cotizacion);
            redis.opsForValue().set("quote:" + symbol, json, ttl);
        } catch (Exception ignored) {}
    }

    @Override
    public void evict(String symbol) {
        redis.delete("quote:" + symbol);
    }
}
