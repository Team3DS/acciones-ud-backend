package co.edu.udistrital.acciones.m6portafolio.integration;

import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class M6SeguridadIntegrationTest extends AbstractIntegrationTest {

    private static final String PORTAFOLIO = "/api/v1/portfolio/1";

    @Test
    void actuatorSigueSiendoPublico() {
        ResponseEntity<Map> respuesta = rest.getForEntity("/actuator/health", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsEntry("status", "UP");
    }

    @Test
    void apiDocsSigueSiendoPublico() {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("bearerAuth");
    }

    @Test
    void sinTokenResponde401EnProblemDetailConTraceId() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "m6-seguridad-401");

        ResponseEntity<String> respuesta = rest.exchange(
                PORTAFOLIO, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).isEqualTo("m6-seguridad-401");
        assertThat(respuesta.getBody())
                .contains("\"status\":401")
                .contains("\"path\":\"" + PORTAFOLIO + "\"")
                .contains("\"traceId\":\"m6-seguridad-401\"");
    }

    @Test
    void unTokenInvalidoResponde401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("esto-no-es-un-jwt");

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/positions/1", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody()).contains("\"status\":401");
    }

    @Test
    void unTokenValidoSuperaLaSeguridadYResponde404EnRutaInexistente() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.usuario());

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/ruta-inexistente", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getBody())
                .contains("\"status\":404")
                .contains("\"path\":\"/api/v1/ruta-inexistente\"");
    }
}
