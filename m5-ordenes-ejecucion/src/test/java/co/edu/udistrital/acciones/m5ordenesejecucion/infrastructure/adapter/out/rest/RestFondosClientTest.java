package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestFondosClientTest {

    @Mock
    RestTemplate restTemplate;

    private static final String SECRETO = "clave-de-prueba-de-al-menos-32-caracteres";

    private RestFondosClient client;

    @BeforeEach
    void setUp() {
        client = new RestFondosClient(restTemplate, "http://m2-cuentas-fondos:8082", new EmisorTokenInterno(new JwtProperties(SECRETO)));
    }

    @Test
    void reservarFondos_retornaReservationIdCuandoRespuestaExitosa() {
        Map<String, Object> response = Map.of("reservationId", 456L);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        Long result = client.reservarFondos("order-1", 100L, new BigDecimal("1500.00"), "idem-key-1");

        assertEquals(456L, result);
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void reservarFondos_lanzaErrorCuandoM2RechazaOFalla() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("409 Fondos insuficientes"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> client.reservarFondos("order-1", 100L, new BigDecimal("1500.00"), "idem-key-1"));
        assertTrue(ex.getMessage().contains("Fondos insuficientes"));
    }

    @Test
    void reservarFondos_lanzaErrorSiM2NoDevuelveReservationId() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("status", "ACTIVA"));

        assertThrows(IllegalStateException.class,
                () -> client.reservarFondos("order-1", 100L, new BigDecimal("1500.00"), "idem-key-1"));
    }

    @Test
    void reservarFondos_enviaHeadersCorrectos() {
        Map<String, Object> response = Map.of("reservationId", 1L);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        client.reservarFondos("order-1", 100L, new BigDecimal("1500.00"), "idem-key-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertEquals("100", headers.getFirst("X-User-Id"));
        assertEquals("USER", headers.getFirst("X-User-Roles"));
    }

    @Test
    void reservarFondos_enviaBodyCorrecto() {
        Map<String, Object> response = Map.of("reservationId", 1L);
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal amount = new BigDecimal("1500.00");
        client.reservarFondos("order-1", 100L, amount, "idem-key-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("order-1", body.get("orderId"));
        assertEquals(100L, body.get("userId"));
        assertEquals(amount, body.get("amount"));
        assertEquals("idem-key-1", body.get("idempotencyKey"));
    }

    @Test
    void confirmarReserva_llamaEndpointCorrecto() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of());

        client.confirmarReserva(456L, 100L, "exec-1", new BigDecimal("1500.00"), true, 1);

        verify(restTemplate).postForObject(
                eq("http://m2-cuentas-fondos:8082/api/v1/funds/reservations/456/confirm"),
                any(HttpEntity.class),
                eq(Map.class));
    }

    @Test
    void confirmarReserva_enviaBodyCorrecto() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of());

        client.confirmarReserva(456L, 100L, "exec-1", new BigDecimal("1500.00"), true, 1);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("exec-1", body.get("executionId"));
        assertEquals(new BigDecimal("1500.00"), body.get("amount"));
    }

    @Test
    void confirmarReserva_propagaFalloRemoto() {
        doThrow(new RuntimeException("Error")).when(restTemplate)
                .postForObject(anyString(), any(HttpEntity.class), eq(Map.class));

        assertThrows(IllegalStateException.class, () -> client.confirmarReserva(456L, 100L, "exec-1", new BigDecimal("1500.00"), true, 1));
    }

    @Test
    void liberarReserva_llamaEndpointCorrecto() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of());

        client.liberarReserva(456L, 100L, "Orden cancelada");

        verify(restTemplate).postForObject(
                eq("http://m2-cuentas-fondos:8082/api/v1/funds/reservations/456/release"),
                any(HttpEntity.class),
                eq(Map.class));
    }

    @Test
    void liberarReserva_enviaReasonEnBody() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of());

        client.liberarReserva(456L, 100L, "Fondos insuficientes");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("Fondos insuficientes", body.get("reason"));
    }

    @Test
    void liberarReserva_propagaFalloRemoto() {
        doThrow(new RuntimeException("Error")).when(restTemplate)
                .postForObject(anyString(), any(HttpEntity.class), eq(Map.class));

        assertThrows(IllegalStateException.class, () -> client.liberarReserva(456L, 100L, "Error test"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reservaVaConElTokenDelDuenoDeLaOrden() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(Map.of("reservationId", 1L));

        client.reservarFondos("order-1", 100L, new BigDecimal("1500.00"), "idem-key-1");

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));
        String autorizacion = captor.getValue().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(autorizacion);
        assertTrue(autorizacion.startsWith("Bearer "));
        Long usuario = new JwtTokenValidator(new JwtProperties(SECRETO)).validar(autorizacion.substring(7)).userId();
        assertEquals(100L, usuario);
    }
}
