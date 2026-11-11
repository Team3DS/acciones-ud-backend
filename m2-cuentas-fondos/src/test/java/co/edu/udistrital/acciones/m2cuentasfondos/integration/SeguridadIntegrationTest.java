package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SeguridadIntegrationTest extends AbstractIntegrationTest {

    @Test
    void actuatorEsPublico() {
        ResponseEntity<Map> respuesta = rest.getForEntity("/actuator/health", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void sinTokenResponde401EnProblemDetail() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "trace-401");

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/accounts/1/balance", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getBody())
                .contains("\"status\":401")
                .contains("\"traceId\":\"trace-401\"")
                .contains("\"path\":\"/api/v1/accounts/1/balance\"");
    }

    @Test
    void conTokenValidoSinControladorResponde404EnProblemDetail() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TokenTestFactory.token(1, "INVERSIONISTA"));

        ResponseEntity<String> respuesta = rest.exchange(
                "/api/v1/accounts/1/balance", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getHeaders().getContentType())
                .isNotNull()
                .matches(tipo -> tipo.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(respuesta.getBody())
                .contains("\"status\":404")
                .contains("\"path\":\"/api/v1/accounts/1/balance\"");
    }

    @Test
    void depositarSinRolAdministradorResponde403() {
        HttpHeaders headers = cabecerasJson();
        headers.setBearerAuth(TokenTestFactory.token(1, "INVERSIONISTA"));

        ResponseEntity<String> respuesta = rest.postForEntity(
                "/api/v1/funds/deposits", new HttpEntity<>("{}", headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody()).contains("\"status\":403");
    }

    private HttpHeaders cabecerasJson() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
