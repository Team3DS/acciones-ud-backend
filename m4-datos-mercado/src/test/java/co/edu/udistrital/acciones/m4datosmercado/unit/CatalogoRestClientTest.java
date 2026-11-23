package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.rest.CatalogoRestClient;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.EmisorTokenServicio;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoRestClientTest {

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
        WebClient.Builder realBuilder = WebClient.builder();
        WebClient.Builder builderSpy = spy(realBuilder);
        lenient().when(builderSpy.baseUrl(anyString())).thenReturn(builderSpy);
        lenient().when(builderSpy.build()).thenReturn(webClient);
        client = new CatalogoRestClient(builderSpy, "http://m3:8083", 5000,
                new EmisorTokenServicio(new JwtProperties("clave-de-prueba-de-al-menos-32-caracteres")));
    }

    @Test
    void validarSimboloRetornaTrueSiValido() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("validos", List.of("AAPL"))));

        boolean result = client.validarSimbolo("AAPL");

        assertTrue(result);
    }

    @Test
    void validarSimboloRetornaFalseSiInvalido() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("validos", List.of("TSLA"))));

        boolean result = client.validarSimbolo("AAPL");

        assertFalse(result);
    }

    @Test
    void validarSimboloRetornaFalseSiError() {
        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(WebClientResponseException.create(500, "Error", null, null, null)));

        boolean result = client.validarSimbolo("AAPL");

        assertFalse(result);
    }
}
