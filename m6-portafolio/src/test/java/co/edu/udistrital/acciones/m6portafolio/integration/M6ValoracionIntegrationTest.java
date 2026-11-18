package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ValorarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class M6ValoracionIntegrationTest extends AbstractIntegrationTest {

    private static final long PRIMER_USUARIO = 6801L;
    private static final long SEGUNDO_USUARIO = 6802L;
    private static final String SYMBOL = "ZVAL";
    private static final Instant COMPRA_EN = Instant.parse("2026-05-02T14:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ValorarPosicionesUseCase valorarPosiciones;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void prepararMercadoAbierto() {
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/market/status"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("{\"mercadoMic\":\"XNYS\",\"estado\":\"ABIERTO\","
                                + "\"consultadoEn\":\"2026-05-02T14:05:00Z\"}")));
    }

    @AfterEach
    void limpiarEscenario() {
        jdbc.update("""
                delete from outbox_evento where aggregate_id in (
                    select cast(id as varchar) from posicion where user_id in (?, ?)
                )
                """, PRIMER_USUARIO, SEGUNDO_USUARIO);
        jdbc.update("""
                delete from movimiento_posicion where posicion_id in (
                    select id from posicion where user_id in (?, ?)
                )
                """, PRIMER_USUARIO, SEGUNDO_USUARIO);
        jdbc.update("""
                delete from reserva_posicion where posicion_id in (
                    select id from posicion where user_id in (?, ?)
                )
                """, PRIMER_USUARIO, SEGUNDO_USUARIO);
        jdbc.update("delete from posicion where user_id in (?, ?)", PRIMER_USUARIO, SEGUNDO_USUARIO);
    }

    @Test
    void valoraLasPosicionesAbiertasYNoRepiteElEventoConElMismoPrecio() throws Exception {
        comprar(PRIMER_USUARIO, "ORD-6801", "EXE-6801", "10");
        comprar(SEGUNDO_USUARIO, "ORD-6802", "EXE-6802", "5");
        MERCADO.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("[{\"symbol\":\"ZVAL\",\"price\":200.00,"
                                + "\"timestamp\":\"2026-05-02T14:04:00Z\"}]")));

        assertThat(valorarPosiciones.ejecutar()).isEqualTo(2);

        Map<String, Object> posicion = jdbc.queryForMap("""
                select id, precio_actual, valor_actual, pnl_no_realizado
                from posicion where user_id = ? and symbol = ?
                """, PRIMER_USUARIO, SYMBOL);
        assertThat(numero(posicion, "precio_actual")).isEqualByComparingTo("200");
        assertThat(numero(posicion, "valor_actual")).isEqualByComparingTo("2000");
        assertThat(numero(posicion, "pnl_no_realizado")).isEqualByComparingTo("150");
        assertThat(eventos()).isEqualTo(2);

        JsonNode payload = objectMapper.readTree(jdbc.queryForObject("""
                select payload from outbox_evento where event_type = ? and aggregate_id = ?
                """, String.class, RoutingKeys.POSICION_VALORADA, posicion.get("id").toString()));
        assertThat(payload.get("userId").asLong()).isEqualTo(PRIMER_USUARIO);
        assertThat(payload.get("symbol").asText()).isEqualTo(SYMBOL);
        assertThat(payload.get("quantity").decimalValue()).isEqualByComparingTo("10");
        assertThat(payload.get("price").decimalValue()).isEqualByComparingTo("200");
        assertThat(payload.get("marketValue").decimalValue()).isEqualByComparingTo("2000");
        assertThat(payload.get("unrealizedPnl").decimalValue()).isEqualByComparingTo("150");
        assertThat(payload.get("valuedAt").asText()).isNotBlank();

        assertThat(valorarPosiciones.ejecutar()).isZero();
        assertThat(eventos()).isEqualTo(2);
    }

    private void comprar(long userId, String orderId, String executionId, String quantity) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("orderId", orderId);
        cuerpo.put("executionId", executionId);
        cuerpo.put("userId", userId);
        cuerpo.put("symbol", SYMBOL);
        cuerpo.put("side", "BUY");
        cuerpo.put("quantity", new BigDecimal(quantity));
        cuerpo.put("price", new BigDecimal("185.00"));
        cuerpo.put("executedAt", COMPRA_EN.toString());

        ResponseEntity<Map> respuesta = rest.exchange("/api/v1/positions/updates", HttpMethod.POST,
                new HttpEntity<>(cuerpo, cabeceras(userId)), Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private int eventos() {
        String marcadores = String.join(",", Collections.nCopies(2, "?"));
        return jdbc.queryForObject("""
                select count(*) from outbox_evento where event_type = ? and aggregate_id in (
                    select cast(id as varchar) from posicion where user_id in (%s)
                )
                """.formatted(marcadores), Integer.class,
                RoutingKeys.POSICION_VALORADA, PRIMER_USUARIO, SEGUNDO_USUARIO);
    }

    private static HttpHeaders cabeceras(long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static BigDecimal numero(Map<String, Object> fila, String columna) {
        return new BigDecimal(fila.get(columna).toString());
    }
}
