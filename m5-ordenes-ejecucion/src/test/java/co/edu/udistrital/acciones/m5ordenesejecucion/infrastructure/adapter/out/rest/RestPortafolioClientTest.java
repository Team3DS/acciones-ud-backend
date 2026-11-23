package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.EmisorTokenInterno;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtTokenValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestPortafolioClientTest {

    @Mock
    RestTemplate restTemplate;

    private static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres";

    private RestPortafolioClient client;

    @BeforeEach
    void setUp() {
        client = new RestPortafolioClient(restTemplate, "http://m6-portafolio:8086", new EmisorTokenInterno(new JwtProperties(SECRETO)));
    }

    @Test
    void reservarPosicion_retornaReservationIdCuandoRespuestaExitosa() {
        Map<String, Object> response = Map.of("reservationId", 123L);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        Long result = client.reservarPosicion("order-1", 100L, "AAPL", 10, "idem-key-1");

        assertEquals(123L, result);
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void reservarPosicion_lanzaErrorCuandoM6RechazaOFalla() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("409 Posición insuficiente"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.reservarPosicion("order-1", 100L, "AAPL", 10, "idem-key-1"));
        assertTrue(ex.getMessage().contains("Posición insuficiente"));
    }

    @Test
    void reservarPosicion_lanzaErrorSiM6NoDevuelveReservationId() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> client.reservarPosicion("order-1", 100L, "AAPL", 10, "idem-key-1"));
    }

    @Test
    void reservarPosicion_enviaHeadersCorrectos() {
        Map<String, Object> response = Map.of("reservationId", 1L);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        client.reservarPosicion("order-1", 100L, "AAPL", 10, "idem-key-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("100", headers.getFirst("X-User-Id"));
        assertEquals("USER", headers.getFirst("X-User-Roles"));
    }

    @Test
    void actualizarPosicion_retornaAverageCostCuandoRespuestaExitosa() {
        Map<String, Object> position = Map.of("averageCost", "150.50");
        Map<String, Object> response = Map.of("position", position);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal result = client.actualizarPosicion(
                "order-1", "exec-1", 100L, "AAPL", LadoOrden.COMPRA, 10,
                new BigDecimal("150.00"), Instant.now(), "event-1");

        assertEquals(new BigDecimal("150.50"), result);
    }

    @Test
    void actualizarPosicion_retornaPrecioEjecucionCuandoFaltaAverageCost() {
        Map<String, Object> position = Map.of();
        Map<String, Object> response = Map.of("position", position);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal price = new BigDecimal("150.00");
        BigDecimal result = client.actualizarPosicion(
                "order-1", "exec-1", 100L, "AAPL", LadoOrden.VENTA, 10,
                price, Instant.now(), "event-1");

        assertEquals(price, result);
    }

    @Test
    void actualizarPosicion_retornaPrecioEjecucionCuandoExcepcion() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Timeout"));

        BigDecimal price = new BigDecimal("150.00");
        BigDecimal result = client.actualizarPosicion(
                "order-1", "exec-1", 100L, "AAPL", LadoOrden.COMPRA, 10,
                price, Instant.now(), "event-1");

        assertEquals(price, result);
    }

    @Test
    void actualizarPosicion_enviaSideCorrectoParaCompra() {
        Map<String, Object> response = Map.of("position", Map.of("averageCost", "150.00"));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        client.actualizarPosicion("order-1", "exec-1", 100L, "AAPL", LadoOrden.COMPRA, 10,
                new BigDecimal("150.00"), Instant.now(), "event-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("COMPRA", body.get("side"));
    }

    @Test
    void actualizarPosicion_enviaSideCorrectoParaVenta() {
        Map<String, Object> response = Map.of("position", Map.of("averageCost", "150.00"));
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        client.actualizarPosicion("order-1", "exec-1", 100L, "AAPL", LadoOrden.VENTA, 10,
                new BigDecimal("150.00"), Instant.now(), "event-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("VENTA", body.get("side"));
    }

    @Test
    void liberarReserva_llamaEndpointCorrecto() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of());

        client.liberarReserva(123L, "Orden cancelada");

        verify(restTemplate).postForObject(
                eq("http://m6-portafolio:8086/api/v1/positions/reservations/123/release"),
                any(HttpEntity.class),
                eq(Map.class));
    }

    @Test
    void liberarReserva_noPropagaExcepcion() {
        doThrow(new RuntimeException("Error")).when(restTemplate)
                .postForObject(anyString(), any(HttpEntity.class), eq(Map.class));

        assertDoesNotThrow(() -> client.liberarReserva(123L, "Error test"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservaVaConElTokenDelDuenoDeLaOrden() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("reservationId", 1L));

        client.reservarPosicion("order-1", 100L, "AAPL", 10, "idem-key-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String autorizacion = captor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(autorizacion);
        assertTrue(autorizacion.startsWith("Bearer "));
        Long usuario = new JwtTokenValidator(new JwtProperties(SECRETO)).validar(autorizacion.substring(7)).userId();
        assertEquals(100L, usuario);
    }
}
