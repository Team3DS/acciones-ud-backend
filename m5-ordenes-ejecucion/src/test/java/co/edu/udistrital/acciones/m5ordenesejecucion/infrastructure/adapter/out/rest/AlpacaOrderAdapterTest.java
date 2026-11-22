package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlpacaOrderAdapterTest {

    @Mock
    RestTemplate restTemplate;

    private AlpacaOrderAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AlpacaOrderAdapter(
                restTemplate,
                "https://paper-api.alpaca.markets",
                "test-api-key",
                "test-api-secret");
    }

    @Test
    void enviarOrdin_conApiKeys_llamaEndpointAlpaca() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.MARKET, VigenciaOrden.DIA,
                null, null, UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        AlpacaOrderPort.AlpacaOrderResponse result = adapter.enviarOrden(request);

        assertEquals("alpaca-123", result.id());
        assertEquals("accepted", result.status());
        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void enviarOrdin_conApiKeys_enviaHeadersAutenticacion() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.MARKET, VigenciaOrden.DIA,
                null, null, UUID.randomUUID());

        adapter.enviarOrden(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals("test-api-key", headers.getFirst("APCA-API-KEY-ID"));
        assertEquals("test-api-secret", headers.getFirst("APCA-API-SECRET-KEY"));
    }

    @Test
    void enviarOrdin_conApiKeys_enviaBodyCorrectoMarket() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.MARKET, VigenciaOrden.DIA,
                null, null, UUID.randomUUID());

        adapter.enviarOrden(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("AAPL", body.get("symbol"));
        assertEquals("10", body.get("qty"));
        assertEquals("buy", body.get("side"));
        assertEquals("market", body.get("type"));
        assertEquals("day", body.get("time_in_force"));
    }

    @Test
    void enviarOrdin_conApiKeys_enviaBodyCorrectoLimit() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal limitPrice = new BigDecimal("150.00");
        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.LIMIT, VigenciaOrden.DIA,
                limitPrice, null, UUID.randomUUID());

        adapter.enviarOrden(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("limit", body.get("type"));
        assertEquals("150.00", body.get("limit_price"));
    }

    @Test
    void enviarOrdin_conApiKeys_enviaBodyCorrectoStop() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal stopPrice = new BigDecimal("140.00");
        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.VENTA, TipoOrden.STOP_LOSS, VigenciaOrden.DIA,
                null, stopPrice, UUID.randomUUID());

        adapter.enviarOrden(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("stop", body.get("type"));
        assertEquals("140.00", body.get("stop_price"));
    }

    @Test
    void enviarOrdin_conApiKeys_enviaBodyCorrectoStopLimit() {
        Map<String, Object> response = Map.of("id", "alpaca-123", "status", "accepted");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(response);

        BigDecimal limitPrice = new BigDecimal("150.00");
        BigDecimal stopPrice = new BigDecimal("140.00");
        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.TAKE_PROFIT, VigenciaOrden.HASTA_CANCELACION,
                limitPrice, stopPrice, UUID.randomUUID());

        adapter.enviarOrden(request);

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(anyString(), captor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
        assertEquals("stop_limit", body.get("type"));
        assertEquals("150.00", body.get("limit_price"));
        assertEquals("140.00", body.get("stop_price"));
        assertEquals("gtc", body.get("time_in_force"));
    }

    @Test
    void enviarOrdin_sinApiKeys_usaSimulador() {
        AlpacaOrderAdapter adapterSinKeys = new AlpacaOrderAdapter(
                restTemplate, "https://paper-api.alpaca.markets", "", "");

        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.MARKET, VigenciaOrden.DIA,
                null, null, UUID.randomUUID());

        AlpacaOrderPort.AlpacaOrderResponse result = adapterSinKeys.enviarOrden(request);

        assertTrue(result.id().startsWith("alpaca-paper-"));
        assertEquals("accepted", result.status());
        assertEquals("AAPL", result.symbol());
        assertEquals(10, result.qty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void enviarOrdin_conApiKeysExcepcion_usaSimulador() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        AlpacaOrderPort.AlpacaOrderRequest request = new AlpacaOrderPort.AlpacaOrderRequest(
                "AAPL", 10, LadoOrden.COMPRA, TipoOrden.MARKET, VigenciaOrden.DIA,
                null, null, UUID.randomUUID());

        AlpacaOrderPort.AlpacaOrderResponse result = adapter.enviarOrden(request);

        assertTrue(result.id().startsWith("alpaca-paper-"));
        assertEquals("accepted", result.status());
    }

    @Test
    void cancelarOrden_conApiKeys_llamaEndpointDelete() {
        adapter.cancelarOrden("alpaca-123");

        verify(restTemplate).exchange(
                eq("https://paper-api.alpaca.markets/v2/orders/alpaca-123"),
                eq(HttpMethod.DELETE),
                any(HttpEntity.class),
                eq(Void.class));
    }

    @Test
    void cancelarOrden_conApiKeys_enviaHeadersAutenticacion() {
        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        adapter.cancelarOrden("alpaca-123");

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.DELETE), captor.capture(), eq(Void.class));

        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals("test-api-key", headers.getFirst("APCA-API-KEY-ID"));
        assertEquals("test-api-secret", headers.getFirst("APCA-API-SECRET-KEY"));
    }

    @Test
    void cancelarOrden_sinApiKeys_noHaceNada() {
        AlpacaOrderAdapter adapterSinKeys = new AlpacaOrderAdapter(
                restTemplate, "https://paper-api.alpaca.markets", "", "");

        assertDoesNotThrow(() -> adapterSinKeys.cancelarOrden("alpaca-123"));
        verifyNoInteractions(restTemplate);
    }

    @Test
    void cancelarOrden_conApiKeysExcepcion_noPropaga() {
        doThrow(new RuntimeException("Network error")).when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        assertDoesNotThrow(() -> adapter.cancelarOrden("alpaca-123"));
    }

    @Test
    void consultarOrden_conApiKeys_retornaRespuestaMapeada() {
        Map<String, Object> response = Map.of(
                "status", "filled",
                "symbol", "AAPL",
                "qty", "10",
                "filled_avg_price", "150.50",
                "filled_qty", "10");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(response));

        AlpacaOrderPort.AlpacaOrderResponse result = adapter.consultarOrden("alpaca-123");

        assertEquals("alpaca-123", result.id());
        assertEquals("filled", result.status());
        assertEquals("AAPL", result.symbol());
        assertEquals(10, result.qty());
        assertEquals(new BigDecimal("150.50"), result.filledAvgPrice());
        assertEquals(10, result.filledQty());
    }

    @Test
    void consultarOrden_sinApiKeys_retornaRespuestaPorDefecto() {
        AlpacaOrderAdapter adapterSinKeys = new AlpacaOrderAdapter(
                restTemplate, "https://paper-api.alpaca.markets", "", "");

        AlpacaOrderPort.AlpacaOrderResponse result = adapterSinKeys.consultarOrden("alpaca-123");

        assertEquals("alpaca-123", result.id());
        assertEquals("accepted", result.status());
        assertEquals("AAPL", result.symbol());
        assertEquals(1, result.qty());
        verifyNoInteractions(restTemplate);
    }

    @Test
    void consultarOrden_conApiKeysExcepcion_retornaRespuestaPorDefecto() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        AlpacaOrderPort.AlpacaOrderResponse result = adapter.consultarOrden("alpaca-123");

        assertEquals("alpaca-123", result.id());
        assertEquals("accepted", result.status());
    }
}