package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class M10SeguridadIntegrationTest extends AbstractIntegrationTest {

    private static final String MI_SUSCRIPCION = "/api/v1/subscriptions/me";
    private static final String ENTITLEMENT = "/internal/subscriptions/1/entitlement";

    @Test
    void actuatorSigueSiendoPublico() {
        ResponseEntity<Map> respuesta = rest.getForEntity("/actuator/health", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsEntry("status", "UP");
    }

    @Test
    void apiDocsPublicaLosDosEsquemasDeSeguridad() {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody())
                .contains("bearerAuth")
                .contains("tokenInterno");
    }

    @Test
    void elCatalogoDePlanesEsPublico() {
        ResponseEntity<String> respuesta = rest.getForEntity("/api/v1/payments/plans", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("PREMIUM");
    }

    @Test
    void elWebhookDeStripeEsPublico() {
        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/payments/webhook/stripe", HttpMethod.POST,
                new HttpEntity<>(new HttpHeaders()), String.class);

        assertThat(respuesta.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void sinTokenResponde401EnProblemDetailConTraceId() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "m10-seguridad-401");

        ResponseEntity<String> respuesta = rest.exchange(
                MI_SUSCRIPCION, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).isEqualTo("m10-seguridad-401");
        assertThat(respuesta.getBody())
                .contains("\"status\":401")
                .contains("\"path\":\"" + MI_SUSCRIPCION + "\"")
                .contains("\"traceId\":\"m10-seguridad-401\"");
    }

    @Test
    void unJwtDeUsuarioNoSirveParaLasRutasInternas() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.usuario());

        ResponseEntity<String> respuesta = rest.exchange(
                ENTITLEMENT, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).contains("\"status\":401");
    }

    @Test
    void unTokenInternoIncorrectoResponde401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("f".repeat(64));

        ResponseEntity<String> respuesta = rest.exchange(
                ENTITLEMENT, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void elTokenInternoCorrectoSuperaLaSeguridad() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.TOKEN_INTERNO);

        ResponseEntity<String> respuesta = rest.exchange(
                ENTITLEMENT, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody())
                .contains("\"active\":false")
                .contains("\"validUntil\":null");
    }

    @Test
    void unJwtValidoSuperaLaSeguridadYResponde404EnRutaInexistente() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.usuario());

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/ruta-inexistente", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).contains("\"status\":404");
    }
}
