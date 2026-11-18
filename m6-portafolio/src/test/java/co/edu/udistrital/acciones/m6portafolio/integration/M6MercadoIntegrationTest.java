package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado.CacheCotizaciones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class M6MercadoIntegrationTest extends AbstractIntegrationTest {

    private static final Instant EJECUTADA_EN = Instant.parse("2026-03-01T10:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheCotizaciones cache;

    @BeforeEach
    void prepararMercado() {
        MERCADO.resetAll();
    }

    @AfterEach
    void limpiarEscenario() {
        cache.limpiar();
        jdbc.update("""
                delete from outbox_evento where aggregate_id in (
                    select cast(id as varchar) from posicion where user_id in (6701, 6702, 6703)
                )
                """);
        jdbc.update("delete from movimiento_posicion where posicion_id in (select id from posicion where user_id in (6701, 6702, 6703))");
        jdbc.update("delete from reserva_posicion where posicion_id in (select id from posicion where user_id in (6701, 6702, 6703))");
        jdbc.update("delete from posicion where user_id in (6701, 6702, 6703)");
    }

    @Test
    void elPortafolioUsaLaCotizacionVivaDeM4() {
        long userId = 6701L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6701", "EXE-6701");
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .withQueryParam("symbols", equalTo("AAPL"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("[{\"symbol\":\"AAPL\",\"price\":200.00,"
                                + "\"timestamp\":\"2026-03-01T10:01:00Z\",\"extra\":\"ignorado\"}]")));

        ResponseEntity<Map> respuesta = portafolio(token, userId);

        assertThat(numero(respuesta.getBody(), "totalValue")).isEqualByComparingTo("2000");
        assertThat(numero(respuesta.getBody(), "unrealizedPnl")).isEqualByComparingTo("150");
        assertThat(respuesta.getBody().get("stale")).isEqualTo(false);
        MERCADO.verify(getRequestedFor(urlPathEqualTo("/api/v1/quotes/latest"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("X-Correlation-ID", matching(".+")));
    }

    @Test
    void conservaElPrecioGuardadoConStaleCuandoM4Falla() {
        long userId = 6702L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6702", "EXE-6702");
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withStatus(500)));

        ResponseEntity<Map> respuesta = portafolio(token, userId);

        assertThat(numero(respuesta.getBody(), "totalValue")).isEqualByComparingTo("1850");
        assertThat(numero(respuesta.getBody(), "unrealizedPnl")).isEqualByComparingTo("0");
        assertThat(respuesta.getBody().get("stale")).isEqualTo(true);
    }

    @Test
    void unErrorClienteEnElLoteConsultaCadaSimboloPorSeparado() {
        long userId = 6703L;
        String token = TokenTestFactory.token(userId, "INVERSIONISTA");
        comprar(token, userId, "ORD-6703", "EXE-6703");
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withStatus(404)));
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/AAPL"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"symbol\":\"AAPL\",\"price\":201.00,"
                                + "\"timestamp\":\"2026-03-01T10:01:00Z\"}")));

        ResponseEntity<Map> respuesta = portafolio(token, userId);

        assertThat(numero(respuesta.getBody(), "totalValue")).isEqualByComparingTo("2010");
        assertThat(respuesta.getBody().get("stale")).isEqualTo(false);
    }

    private void comprar(String token, long userId, String orderId, String executionId) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", "AAPL");
        cuerpo.put("side", "BUY");
        cuerpo.put("quantity", BigDecimal.TEN);
        cuerpo.put("price", new BigDecimal("185.00"));
        cuerpo.put("executedAt", EJECUTADA_EN.toString());
        rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(token)), Map.class);
    }

    private ResponseEntity<Map> portafolio(String token, long userId) {
        return rest.exchange("/api/v1/portfolio/" + userId, HttpMethod.GET,
                new HttpEntity<>(cabeceras(token)), Map.class);
    }

    private static HttpHeaders cabeceras(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static BigDecimal numero(Map<?, ?> respuesta, String campo) {
        return new BigDecimal(respuesta.get(campo).toString());
    }
}
