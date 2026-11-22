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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

class M10CheckoutIntegrationTest extends AbstractIntegrationTest {

    private static final long USUARIO = 770001L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void preparar() {
        STRIPE.resetAll();
        jdbc.update("DELETE FROM sesion_checkout WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id = ?", USUARIO);
        stubPrecio();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM sesion_checkout WHERE user_id = ?", USUARIO);
        jdbc.update("DELETE FROM suscripcion WHERE user_id = ?", USUARIO);
    }

    @Test
    void elCheckoutCreaLaSesionYLaGuardaAbierta() throws Exception {
        stubSesion("cs_test_int_1", "https://stripe.test/pay/cs_test_int_1");

        ResponseEntity<String> respuesta = iniciar(USUARIO, "clave-int-1", "MENSUAL");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode cuerpo = json.readTree(respuesta.getBody());
        assertThat(cuerpo.path("sessionId").asText()).isEqualTo("cs_test_int_1");
        assertThat(cuerpo.path("checkoutUrl").asText()).isEqualTo("https://stripe.test/pay/cs_test_int_1");

        Integer abiertas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sesion_checkout WHERE user_id = ? AND estado = 'ABIERTA'",
                Integer.class, USUARIO);
        assertThat(abiertas).isEqualTo(1);
    }

    @Test
    void dosPeticionesConcurrentesCreanUnaSolaSesion() throws Exception {
        stubSesion("cs_test_concurrente", "https://stripe.test/pay/cs_test_concurrente");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<ResponseEntity<String>> primera = () -> iniciar(USUARIO, "clave-a", "MENSUAL");
            Callable<ResponseEntity<String>> segunda = () -> iniciar(USUARIO, "clave-b", "MENSUAL");
            List<Future<ResponseEntity<String>>> resultados = pool.invokeAll(List.of(primera, segunda));
            List<Integer> estados = new ArrayList<>();
            for (Future<ResponseEntity<String>> futuro : resultados) {
                estados.add(futuro.get().getStatusCode().value());
            }
            assertThat(estados).allMatch(estado -> estado == 200 || estado == 201);
        } finally {
            pool.shutdownNow();
        }

        STRIPE.verify(1, postRequestedFor(urlPathEqualTo("/v1/checkout/sessions")));
        Integer abiertas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sesion_checkout WHERE user_id = ? AND estado = 'ABIERTA'",
                Integer.class, USUARIO);
        assertThat(abiertas).isEqualTo(1);
    }

    @Test
    void laMismaClaveReutilizaLaSesionYResponde200() throws Exception {
        stubSesion("cs_test_reuso", "https://stripe.test/pay/cs_test_reuso");

        ResponseEntity<String> primera = iniciar(USUARIO, "clave-reuso", "MENSUAL");
        ResponseEntity<String> segunda = iniciar(USUARIO, "clave-reuso", "MENSUAL");

        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        STRIPE.verify(1, postRequestedFor(urlPathEqualTo("/v1/checkout/sessions")));
    }

    private ResponseEntity<String> iniciar(long userId, String clave, String intervalo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(userId, "INVERSIONISTA"));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", clave);
        return rest.exchange("/api/v1/subscriptions", HttpMethod.POST,
                new HttpEntity<>("{\"interval\":\"" + intervalo + "\"}", headers), String.class);
    }

    private void stubPrecio() {
        STRIPE.stubFor(get(urlPathMatching("/v1/prices.*"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"object":"list","data":[{"id":"price_mensual","object":"price",
                                 "unit_amount":1200,"currency":"usd","lookup_key":"acciones_ud_premium_mensual"}]}
                                """)));
    }

    private void stubSesion(String id, String url) {
        STRIPE.stubFor(post(urlPathEqualTo("/v1/checkout/sessions"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":"%s","object":"checkout.session","url":"%s",
                                 "expires_at":1800000000,"customer":null,"subscription":null}
                                """.formatted(id, url))));
    }
}
