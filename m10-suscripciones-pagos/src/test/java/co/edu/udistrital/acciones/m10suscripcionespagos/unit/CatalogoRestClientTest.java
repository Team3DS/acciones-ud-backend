package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest.CatalogoRestClient;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogoRestClientTest {

    private static final WireMockServer CATALOGO = new WireMockServer(options().dynamicPort());
    private static final String RUTA = "/api/v1/instruments/AAPL/validation";

    private CatalogoRestClient cliente;

    @BeforeAll
    static void iniciar() {
        CATALOGO.start();
    }

    @AfterAll
    static void detener() {
        CATALOGO.stop();
    }

    @BeforeEach
    void preparar() {
        CATALOGO.resetAll();
        cliente = new CatalogoRestClient(restClient(Duration.ofSeconds(5)), emisor());
    }

    @AfterEach
    void limpiarMdc() {
        MDC.remove(CorrelationIdFilter.CLAVE_MDC);
    }

    @Test
    void aceptaUnSimboloValidoIgnorandoCamposDesconocidosYPropagaLasCabeceras() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "corr-catalogo");
        CATALOGO.stubFor(get(urlPathEqualTo(RUTA))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"symbol":"AAPL","valid":true,"reason":"HABILITADO","campoNuevo":123}
                                """)));

        assertThat(cliente.esValido(Simbolo.desde("aapl"))).isTrue();

        CATALOGO.verify(getRequestedFor(urlPathEqualTo(RUTA))
                .withHeader("Authorization", containing("Bearer "))
                .withHeader("X-Correlation-ID", equalTo("corr-catalogo")));
    }

    @Test
    void rechazaUnSimboloQueElCatalogoMarcaComoNoValido() {
        CATALOGO.stubFor(get(urlPathEqualTo("/api/v1/instruments/ZZZZ/validation"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valid\":false}")));

        assertThat(cliente.esValido(Simbolo.desde("ZZZZ"))).isFalse();
    }

    @Test
    void trataUn400DelCatalogoComoSimboloNoValido() {
        CATALOGO.stubFor(get(urlPathEqualTo(RUTA))
                .willReturn(aResponse().withStatus(400)));

        assertThat(cliente.esValido(Simbolo.desde("AAPL"))).isFalse();
    }

    @Test
    void convierteUn500EnCatalogoNoDisponible() {
        CATALOGO.stubFor(get(urlPathEqualTo(RUTA))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> cliente.esValido(Simbolo.desde("AAPL")))
                .isInstanceOf(CatalogoNoDisponibleException.class);
    }

    @Test
    void convierteUnTimeoutEnCatalogoNoDisponible() {
        CatalogoRestClient conTimeoutCorto = new CatalogoRestClient(restClient(Duration.ofSeconds(1)), emisor());
        CATALOGO.stubFor(get(urlPathEqualTo(RUTA))
                .willReturn(aResponse().withFixedDelay(1500).withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valid\":true}")));

        assertThatThrownBy(() -> conTimeoutCorto.esValido(Simbolo.desde("AAPL")))
                .isInstanceOf(CatalogoNoDisponibleException.class);
    }

    private static RestClient restClient(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(CATALOGO.baseUrl()).requestFactory(fabrica).build();
    }

    private static EmisorTokenServicio emisor() {
        return new EmisorTokenServicio(new JwtProperties(TokenTestFactory.SECRETO),
                new ServicioProperties(900000010L, Duration.ofMinutes(5)), Clock.systemUTC());
    }
}
