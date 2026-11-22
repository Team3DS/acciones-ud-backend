package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestDatosMercadoClientTest {

    @Mock
    RestTemplate restTemplate;

    private RestDatosMercadoClient client;

    @BeforeEach
    void setUp() {
        client = new RestDatosMercadoClient(restTemplate, "http://m4-datos-mercado:8084");
    }

    @Test
    void esMercadoAbierto_retornaTrueCuandoEstadoAbierto() {
        Map<String, Object> response = Map.of("estado", "ABIERTO");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        boolean result = client.esMercadoAbierto("NYSE");

        assertTrue(result);
        verify(restTemplate).getForObject(
                eq("http://m4-datos-mercado:8084/api/v1/market/status?market=NYSE"),
                eq(Map.class));
    }

    @Test
    void esMercadoAbierto_retornaFalseCuandoEstadoCerrado() {
        Map<String, Object> response = Map.of("estado", "CERRADO");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        boolean result = client.esMercadoAbierto("NYSE");

        assertFalse(result);
    }

    @Test
    void esMercadoAbierto_retornaTrueCuandoExcepcion() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Timeout"));

        boolean result = client.esMercadoAbierto("NYSE");

        assertTrue(result);
    }

    @Test
    void esMercadoAbierto_retornaTrueCuandoRespuestaNula() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        boolean result = client.esMercadoAbierto("NYSE");

        assertTrue(result);
    }

    @Test
    void esMercadoAbierto_retornaTrueCuandoFaltaCampoEstado() {
        Map<String, Object> response = Map.of("otro", "valor");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        boolean result = client.esMercadoAbierto("NYSE");

        assertTrue(result);
    }

    @Test
    void esMercadoAbierto_caseInsensitive() {
        Map<String, Object> response = Map.of("estado", "abierto");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        boolean result = client.esMercadoAbierto("NYSE");

        assertTrue(result);
    }

    @Test
    void obtenerPrecioEstimado_retornaPrecioCuandoRespuestaExitosa() {
        Map<String, Object> response = Map.of("price", "250.75");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        BigDecimal result = client.obtenerPrecioEstimado("AAPL");

        assertEquals(new BigDecimal("250.75"), result);
        verify(restTemplate).getForObject(
                eq("http://m4-datos-mercado:8084/api/v1/quotes/AAPL"),
                eq(Map.class));
    }

    @Test
    void obtenerPrecioEstimado_retornaPrecioPorDefectoCuandoExcepcion() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Timeout"));

        BigDecimal result = client.obtenerPrecioEstimado("AAPL");

        assertEquals(new BigDecimal("150.00"), result);
    }

    @Test
    void obtenerPrecioEstimado_retornaPrecioPorDefectoCuandoRespuestaNula() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        BigDecimal result = client.obtenerPrecioEstimado("AAPL");

        assertEquals(new BigDecimal("150.00"), result);
    }

    @Test
    void obtenerPrecioEstimado_retornaPrecioPorDefectoCuandoFaltaCampoPrice() {
        Map<String, Object> response = Map.of("symbol", "AAPL");
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        BigDecimal result = client.obtenerPrecioEstimado("AAPL");

        assertEquals(new BigDecimal("150.00"), result);
    }

    @Test
    void obtenerPrecioEstimado_conviertePriceNumero() {
        Map<String, Object> response = Map.of("price", 250.75);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        BigDecimal result = client.obtenerPrecioEstimado("AAPL");

        assertEquals(new BigDecimal("250.75"), result);
    }
}