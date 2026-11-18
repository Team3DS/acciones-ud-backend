package co.edu.udistrital.acciones.m6portafolio.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureObservability(metrics = true)
class M6ArranqueIntegrationTest extends AbstractIntegrationTest {

    private static final String PATRON_UUID =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Test
    void healthRespondeUp() {
        ResponseEntity<Map> respuesta = rest.getForEntity("/actuator/health", Map.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).containsEntry("status", "UP");
    }

    @Test
    void prometheusExponeMetricas() {
        ResponseEntity<String> respuesta = rest.getForEntity("/actuator/prometheus", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    void apiDocsPublicaElTituloDelServicio() {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).contains("M6 Portafolio");
    }

    @Test
    void mantieneElCorrelationIdDeLaPeticion() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "portafolio-2026_ABC");

        ResponseEntity<String> respuesta = rest.exchange(
                "/actuator/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).isEqualTo("portafolio-2026_ABC");
    }

    @Test
    void generaCorrelationIdSiLaPeticionNoLoTrae() {
        ResponseEntity<String> respuesta = rest.getForEntity("/actuator/health", String.class);

        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).matches(PATRON_UUID);
    }
}
