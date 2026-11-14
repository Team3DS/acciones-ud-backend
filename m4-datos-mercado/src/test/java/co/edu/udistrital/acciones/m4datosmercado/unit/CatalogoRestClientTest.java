package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.rest.CatalogoRestClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoRestClientTest {

    @Mock
    WebClient.Builder webClientBuilder;

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersUriSpec uriSpec;

    @Mock
    WebClient.RequestHeadersSpec headersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    CatalogoClientPort client;

    @BeforeEach
    void setUp() {
        client = new CatalogoRestClient(webClientBuilder, "http://m3:8083", 5000);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
    }

    @Test
    void validarSimboloRetornaTrueSiValido() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("validos", List.of("AAPL"))));

        boolean result = client.validarSimbolo("AAPL");

        assertTrue(result);
    }

    @Test
    void validarSimboloRetornaFalseSiInvalido() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("validos", List.of("TSLA"))));

        boolean result = client.validarSimbolo("AAPL");

        assertFalse(result);
    }

    @Test
    void validarSimboloRetornaFalseSiError() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new WebClientResponseException(500, "Error", null, null, null)));

        boolean result = client.validarSimbolo("AAPL");

        assertFalse(result);
    }
}