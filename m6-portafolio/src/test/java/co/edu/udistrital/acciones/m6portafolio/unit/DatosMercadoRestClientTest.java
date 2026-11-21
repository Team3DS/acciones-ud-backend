package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado.CacheCotizaciones;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado.DatosMercadoRestClient;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.M4ClientProperties;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ServicioProperties;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.TokenServicioProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DatosMercadoRestClientTest {

    private static final String BASE_URL = "http://m4";
    private static final String SECRETO = "m6-test-jwt-secret-0123456789abcdef0123456789abcdef0123456789abcdef";

    private MockRestServiceServer servidor;
    private DatosMercadoRestClient cliente;

    @BeforeEach
    void prepararCliente() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        servidor = MockRestServiceServer.bindTo(builder).build();
        TokenServicioProvider token = new TokenServicioProvider(new JwtProperties(SECRETO),
                new ServicioProperties(900000006L, Duration.ofMinutes(5)), Clock.systemUTC());
        cliente = new DatosMercadoRestClient(builder.build(), token,
                new CacheCotizaciones(new M4ClientProperties(BASE_URL, 5, 5), Clock.systemUTC()));
    }

    @AfterEach
    void limpiarMdc() {
        MDC.remove(CorrelationIdFilter.CLAVE_MDC);
    }

    @Test
    void devuelveListaIgnorandoCamposDesconocidosYPropagaLasCabeceras() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "corr-mercado");
        servidor.expect(once(), requestTo(BASE_URL + "/api/v1/quotes/latest?symbols=AAPL"))
                .andExpect(method(GET))
                .andExpect(header("Authorization", org.hamcrest.Matchers.startsWith("Bearer ")))
                .andExpect(header("X-Correlation-ID", "corr-mercado"))
                .andRespond(withSuccess("""
                        [{"symbol":"AAPL","price":200.00,
                          "timestamp":"2026-03-01T10:01:00Z","campoNuevo":"ignorado"}]
                        """, MediaType.APPLICATION_JSON));

        Map<String, CotizacionMercado> cotizaciones = cliente.obtenerCotizaciones(Set.of("aapl"));

        assertThat(cotizaciones).containsKey("AAPL");
        assertThat(cotizaciones.get("AAPL").price()).isEqualByComparingTo("200.00");
        servidor.verify();
    }

    @Test
    void anteUn404DelLoteConsultaCadaSimbolo() {
        servidor.expect(requestTo(BASE_URL + "/api/v1/quotes/latest?symbols=AAPL"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));
        servidor.expect(requestTo(BASE_URL + "/api/v1/quotes/AAPL"))
                .andRespond(withSuccess("""
                        {"symbol":"AAPL","price":201.00,"timestamp":"2026-03-01T10:01:00Z"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, CotizacionMercado> cotizaciones = cliente.obtenerCotizaciones(Set.of("AAPL"));

        assertThat(cotizaciones.get("AAPL").price()).isEqualByComparingTo("201.00");
        servidor.verify();
    }

    @Test
    void convierteUn500EnErrorDeMercadoNoDisponible() {
        servidor.expect(requestTo(BASE_URL + "/api/v1/quotes/latest?symbols=AAPL"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> cliente.obtenerCotizaciones(Set.of("AAPL")))
                .isInstanceOf(DatosMercadoNoDisponibleException.class);
    }

    @Test
    void convierteUnTimeoutEnErrorDeMercadoNoDisponible() {
        servidor.expect(requestTo(BASE_URL + "/api/v1/quotes/latest?symbols=AAPL"))
                .andRespond(request -> {
                    throw new IOException("timeout simulado");
                });

        assertThatThrownBy(() -> cliente.obtenerCotizaciones(Set.of("AAPL")))
                .isInstanceOf(DatosMercadoNoDisponibleException.class);
    }
}
