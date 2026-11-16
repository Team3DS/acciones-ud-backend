package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.persistence.RedisCacheAdapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheAdapterTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    ObjectMapper objectMapper = new ObjectMapper();
    CacheCotizacionesPort adapter;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        adapter = new RedisCacheAdapter(redisTemplate, objectMapper);
    }

    @Test
    void getRetornaEmptySiNoExiste() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("quote:AAPL")).thenReturn(null);

        Optional<Cotizacion> result = adapter.get("AAPL");

        assertTrue(result.isEmpty());
    }

    @Test
    void getRetornaCotizacionSiExiste() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Cotizacion cotizacion = new Cotizacion("AAPL", new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, java.time.Instant.now(), null, null, null, null, null, null);
        String json = objectMapper.writeValueAsString(cotizacion);
        when(valueOps.get("quote:AAPL")).thenReturn(json);

        Optional<Cotizacion> result = adapter.get("AAPL");

        assertTrue(result.isPresent());
        assertEquals("AAPL", result.get().symbol());
    }

    @Test
    void putSerializaYGuarda() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        Cotizacion cotizacion = new Cotizacion("AAPL", new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, java.time.Instant.now(), null, null, null, null, null, null);

        adapter.put("AAPL", cotizacion, Duration.ofSeconds(60));

        verify(valueOps).set(eq("quote:AAPL"), anyString(), eq(Duration.ofSeconds(60)));
    }

    @Test
    void evictEliminaClave() {
        adapter.evict("AAPL");
        verify(redisTemplate).delete("quote:AAPL");
    }
}