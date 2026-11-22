package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class M10WatchlistIntegrationTest extends AbstractIntegrationTest {

    private static final long USUARIO = 99111L;
    private static final String SUB = "sub_watchlist_991";
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-01-01T00:00:00Z");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void preparar() {
        SERVICIOS.resetAll();
        limpiar();
        insertarSuscripcion("ACTIVA");
        stubCatalogo(true);
        stubMercado(200);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM watchlist_item WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id = ?", USUARIO);
    }

    @Test
    void elUsuarioPremiumAgregaVeYQuitaUnSimbolo() throws Exception {
        ResponseEntity<String> alta = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"aapl\"}");
        assertThat(alta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(json.readTree(alta.getBody()).path("symbol").asText()).isEqualTo("AAPL");

        JsonNode lista = json.readTree(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getBody());
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("price").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(lista.get(0).path("stale").asBoolean()).isFalse();

        ResponseEntity<String> baja = peticion(HttpMethod.DELETE,
                "/api/v1/watchlist/items/AAPL", null);
        assertThat(baja.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(json.readTree(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getBody()))
                .isEmpty();
    }

    @Test
    void agregarDosVecesElMismoSimboloNoDuplicaLaFila() {
        ResponseEntity<String> primera = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"AAPL\"}");
        ResponseEntity<String> segunda = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"aapl\"}");

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM watchlist_item WHERE user_id = ?",
                Long.class, USUARIO)).isEqualTo(1L);
    }

    @Test
    void unSimboloRechazadoPorM3Responde422() {
        SERVICIOS.resetAll();
        stubCatalogo(false);
        stubMercado(200);

        ResponseEntity<String> respuesta = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"ZZZZ\"}");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void siM3NoRespondeResponde503() {
        SERVICIOS.resetAll();
        SERVICIOS.stubFor(get(urlPathMatching("/api/v1/instruments/.*/validation"))
                .willReturn(aResponse().withStatus(500)));
        stubMercado(200);

        ResponseEntity<String> respuesta = peticion(HttpMethod.POST, "/api/v1/watchlist/items",
                "{\"symbol\":\"AAPL\"}");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM watchlist_item WHERE user_id = ?",
                Long.class, USUARIO)).isZero();
    }

    @Test
    void siM4NoRespondeLaWatchlistDevuelveStale() throws Exception {
        peticion(HttpMethod.POST, "/api/v1/watchlist/items", "{\"symbol\":\"MSFT\"}");
        SERVICIOS.resetAll();
        stubCatalogo(true);
        SERVICIOS.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withStatus(500)));

        JsonNode lista = json.readTree(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getBody());

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("symbol").asText()).isEqualTo("MSFT");
        assertThat(lista.get(0).path("price").isNull()).isTrue();
        assertThat(lista.get(0).path("stale").asBoolean()).isTrue();
    }

    @Test
    void alPerderElPremiumSeConservanLasFilasYAlRecuperarloReaparecen() throws Exception {
        peticion(HttpMethod.POST, "/api/v1/watchlist/items", "{\"symbol\":\"AAPL\"}");
        cambiarEstado("CANCELADA");

        ResponseEntity<String> sinPremium = peticion(HttpMethod.GET, "/api/v1/watchlist", null);
        assertThat(sinPremium.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(json.readTree(sinPremium.getBody()).path("code").asText())
                .isEqualTo("PREMIUM_REQUERIDO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM watchlist_item WHERE user_id = ?",
                Long.class, USUARIO)).isEqualTo(1L);

        cambiarEstado("ACTIVA");
        JsonNode lista = json.readTree(peticion(HttpMethod.GET, "/api/v1/watchlist", null).getBody());
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).path("symbol").asText()).isEqualTo("AAPL");
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String ruta, String cuerpo) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setBearerAuth(TokenTestFactory.token(USUARIO, "INVERSIONISTA"));
        if (cuerpo != null) {
            cabeceras.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange(ruta, metodo, new HttpEntity<>(cuerpo, cabeceras), String.class);
    }

    private void insertarSuscripcion(String estado) {
        Long precioId = jdbc.queryForObject(
                "SELECT id FROM precio_plan WHERE stripe_lookup_key = ?",
                Long.class, "acciones_ud_premium_mensual");
        jdbc.update("""
                INSERT INTO suscripcion (user_id, precio_plan_id, stripe_subscription_id, estado,
                    periodo_inicio, periodo_fin, cancelar_al_final, creado_en, actualizado_en, version)
                VALUES (?, ?, ?, ?, ?, ?, false, ?, ?, 0)
                """,
                USUARIO, precioId, SUB, estado,
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(FIN, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(INICIO, ZoneOffset.UTC));
    }

    private void cambiarEstado(String estado) {
        jdbc.update("UPDATE suscripcion SET estado = ? WHERE user_id = ?", estado, USUARIO);
    }

    private void stubCatalogo(boolean valido) {
        SERVICIOS.stubFor(get(urlPathMatching("/api/v1/instruments/.*/validation"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"valid\":" + valido + "}")));
    }

    private void stubMercado(int estado) {
        SERVICIOS.stubFor(get(urlPathEqualTo("/api/v1/quotes/latest"))
                .willReturn(aResponse().withStatus(estado)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"symbol":"AAPL","price":200.00,"change":1.00,
                                  "changePct":0.50,"timestamp":"2026-11-22T00:00:00Z"}]
                                """)));
    }
}
