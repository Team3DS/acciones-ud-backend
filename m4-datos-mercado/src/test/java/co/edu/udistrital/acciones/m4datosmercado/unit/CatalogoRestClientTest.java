package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.EstadoMercado;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;
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

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void respuesta(String uri, String variable, Mono<?> cuerpo) {
        lenient().when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq(uri), eq(variable))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn((Mono) cuerpo);
    }

    private static final String VALIDACION = "/api/v1/instruments/{symbol}/validation";
    private static final String MERCADO = "/api/v1/markets/{code}";
    private static final Instant MARTES_10AM_NY = ZonedDateTime.of(2026, 11, 24, 10, 0, 0, 0, ZoneId.of("America/New_York")).toInstant();
    private static final Instant SABADO_10AM_NY = ZonedDateTime.of(2026, 11, 28, 10, 0, 0, 0, ZoneId.of("America/New_York")).toInstant();

    private static Map<String, Object> nyse(Object open, Object close) {
        return Map.of("code", "NYSE", "mic", "XNYS", "tradingHours", Map.of("open", open, "close", close));
    }

    @Test
    void validarSimboloRetornaTrueSiValido() {
        respuesta(VALIDACION, "AAPL", Mono.just(Map.of("symbol", "AAPL", "valid", true)));

        assertTrue(client.validarSimbolo("aapl"));
    }

    @Test
    void validarSimboloRetornaFalseSiInvalido() {
        respuesta(VALIDACION, "AAPL", Mono.just(Map.of("symbol", "AAPL", "valid", false, "reason", "NO_ENCONTRADO")));

        assertFalse(client.validarSimbolo("AAPL"));
    }

    @Test
    void validarSimboloRetornaFalseSiError() {
        respuesta(VALIDACION, "AAPL", Mono.error(WebClientResponseException.create(500, "Error", null, null, null)));

        assertFalse(client.validarSimbolo("AAPL"));
    }

    @Test
    void horariosDeLunesAViernesDesdeElMercadoDeM3() {
        respuesta(MERCADO, "NYSE", Mono.just(nyse("09:30:00", "16:00:00")));

        List<HorarioMercado> horarios = client.obtenerHorariosMercado("nyse");

        assertEquals(5, horarios.size());
        assertEquals("XNYS", horarios.get(0).getMercadoMic());
        assertEquals(LocalTime.of(9, 30), horarios.get(0).getApertura());
        assertEquals(LocalTime.of(16, 0), horarios.get(4).getCierre());
        assertEquals(5, horarios.get(4).getDiaSemana());
    }

    @Test
    void sinMercadoOTodosSeConsultaNyseYAceptaHorasComoArreglo() {
        respuesta(MERCADO, "NYSE", Mono.just(nyse(List.of(9, 30), List.of(16, 0))));

        assertEquals(EstadoMercado.ABIERTO, client.obtenerEstadoMercado("ALL", MARTES_10AM_NY));
        assertEquals(EstadoMercado.CERRADO, client.obtenerEstadoMercado(null, SABADO_10AM_NY));
        assertTrue(client.obtenerHorarioMercado(" ").isPresent());
    }

    @Test
    void sinHorarioEnM3ElMercadoQuedaCerrado() {
        respuesta(MERCADO, "NASDAQ", Mono.just(Map.of("code", "NASDAQ")));

        assertTrue(client.obtenerHorariosMercado("NASDAQ").isEmpty());
        assertEquals(EstadoMercado.CERRADO, client.obtenerEstadoMercado("NASDAQ", MARTES_10AM_NY));
        assertTrue(client.obtenerHorarioMercado("NASDAQ").isEmpty());
    }

    @Test
    void horaInvalidaSeIgnora() {
        respuesta(MERCADO, "NYSE", Mono.just(nyse("abre-temprano", "16:00")));

        assertTrue(client.obtenerHorariosMercado("NYSE").isEmpty());
    }
}
