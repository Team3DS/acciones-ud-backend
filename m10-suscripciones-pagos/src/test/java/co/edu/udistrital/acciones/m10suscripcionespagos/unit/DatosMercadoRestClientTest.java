package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest.CacheCotizaciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest.DatosMercadoRestClient;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.ClientesHttpProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EmisorTokenServicio;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ServicioProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatosMercadoRestClientTest {

    private static final WireMockServer MERCADO = new WireMockServer(options().dynamicPort());
    private static final String LOTE = "/api/v1/quotes/latest";

    private DatosMercadoRestClient cliente;

    @BeforeAll
    static void iniciar() {
        MERCADO.start();
    }

    @AfterAll
    static void detener() {
        MERCADO.stop();
    }

    @BeforeEach
    void preparar() {
        MERCADO.resetAll();
        cliente = new DatosMercadoRestClient(restClient(Duration.ofSeconds(5)), emisor(), cache());
    }

    @AfterEach
    void limpiarMdc() {
        MDC.remove(CorrelationIdFilter.CLAVE_MDC);
    }

    @Test
    void devuelveLaListaIgnorandoCamposDesconocidosYPropagaLasCabeceras() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "corr-mercado");
        MERCADO.stubFor(get(urlPathEqualTo(LOTE))
                .withQueryParam("symbols", equalTo("AAPL"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"symbol":"AAPL","price":200.00,"change":1.50,"changePct":0.75,
                                  "timestamp":"2026-11-22T00:00:00Z","campoNuevo":"ignorado"}]
                                """)));

        Map<String, Cotizacion> cotizaciones = cliente.obtenerCotizaciones(Set.of("aapl"));

        Cotizacion cotizacion = cotizaciones.get("AAPL");
        assertThat(cotizacion.price()).isEqualByComparingTo("200.00");
        assertThat(cotizacion.change()).isEqualByComparingTo("1.50");
        assertThat(cotizacion.changePct()).isEqualByComparingTo("0.75");
        assertThat(cotizacion.timestamp()).isEqualTo(Instant.parse("2026-11-22T00:00:00Z"));
        MERCADO.verify(getRequestedFor(urlPathEqualTo(LOTE))
                .withHeader("Authorization", containing("Bearer "))
                .withHeader("X-Correlation-ID", equalTo("corr-mercado")));
    }

    @Test
    void consultaCadaSimboloSiElLoteResponde4xx() {
        MERCADO.stubFor(get(urlPathEqualTo(LOTE))
                .willReturn(aResponse().withStatus(404)));
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/AAPL"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"symbol\":\"AAPL\",\"price\":201.00,\"campoNuevo\":true}")));

        Map<String, Cotizacion> cotizaciones = cliente.obtenerCotizaciones(Set.of("AAPL"));

        assertThat(cotizaciones.get("AAPL").price()).isEqualByComparingTo("201.00");
    }

    @Test
    void convierteUn500EnDatosMercadoNoDisponible() {
        MERCADO.stubFor(get(urlPathEqualTo(LOTE))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> cliente.obtenerCotizaciones(Set.of("AAPL")))
                .isInstanceOf(DatosMercadoNoDisponibleException.class);
    }

    @Test
    void convierteUnTimeoutEnDatosMercadoNoDisponible() {
        DatosMercadoRestClient conTimeoutCorto = new DatosMercadoRestClient(
                restClient(Duration.ofSeconds(1)), emisor(), cache());
        MERCADO.stubFor(get(urlPathEqualTo(LOTE))
                .willReturn(aResponse().withFixedDelay(1500).withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        assertThatThrownBy(() -> conTimeoutCorto.obtenerCotizaciones(Set.of("AAPL")))
                .isInstanceOf(DatosMercadoNoDisponibleException.class);
    }

    @Test
    void reutilizaLaCotizacionEnLaSegundaConsulta() {
        stubLista();
        cliente.obtenerCotizaciones(Set.of("AAPL"));

        cliente.obtenerCotizaciones(Set.of("AAPL"));

        MERCADO.verify(1, getRequestedFor(urlPathEqualTo(LOTE)));
    }

    private static void stubLista() {
        MERCADO.stubFor(get(urlPathEqualTo(LOTE))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"symbol\":\"AAPL\",\"price\":200.00}]")));
    }

    private static RestClient restClient(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(MERCADO.baseUrl()).requestFactory(fabrica).build();
    }

    private static EmisorTokenServicio emisor() {
        return new EmisorTokenServicio(new JwtProperties(TokenTestFactory.SECRETO),
                new ServicioProperties(900000010L, Duration.ofMinutes(5)), Clock.systemUTC());
    }

    private static CacheCotizaciones cache() {
        return new CacheCotizaciones(
                new ClientesHttpProperties("http://m3", "http://m4", 5, 5), Clock.systemUTC());
    }
}
