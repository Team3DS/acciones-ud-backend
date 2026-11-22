package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureObservability(metrics = true)
class M10ArranqueIntegrationTest extends AbstractIntegrationTest {

    private static final String PATRON_UUID =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private JdbcTemplate jdbc;

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
        assertThat(respuesta.getBody()).contains("M10 Suscripciones y Pagos");
    }

    @Test
    void flywayDejaDosPreciosEnElCatalogo() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM precio_plan", Integer.class);
        assertThat(total).isEqualTo(2);

        List<String> lookupKeys = jdbc.queryForList(
                "SELECT stripe_lookup_key FROM precio_plan ORDER BY stripe_lookup_key", String.class);
        assertThat(lookupKeys).containsExactly(
                "acciones_ud_premium_anual", "acciones_ud_premium_mensual");

        BigDecimal mensual = jdbc.queryForObject(
                "SELECT monto FROM precio_plan WHERE stripe_lookup_key = ?",
                BigDecimal.class, "acciones_ud_premium_mensual");
        BigDecimal anual = jdbc.queryForObject(
                "SELECT monto FROM precio_plan WHERE stripe_lookup_key = ?",
                BigDecimal.class, "acciones_ud_premium_anual");
        assertThat(mensual).isEqualByComparingTo("12.00");
        assertThat(anual).isEqualByComparingTo("120.00");
    }

    @Test
    void mantieneElCorrelationIdDeLaPeticion() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "pagos-2026_ABC");

        ResponseEntity<String> respuesta = rest.exchange(
                "/actuator/health", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).isEqualTo("pagos-2026_ABC");
    }

    @Test
    void generaCorrelationIdSiLaPeticionNoLoTrae() {
        ResponseEntity<String> respuesta = rest.getForEntity("/actuator/health", String.class);

        assertThat(respuesta.getHeaders().getFirst("X-Correlation-ID")).matches(PATRON_UUID);
    }
}
