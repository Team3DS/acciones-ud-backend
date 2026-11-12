package co.edu.udistrital.acciones.m2cuentasfondos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exponeLasOchoRutasConElEsquemaBearer() throws Exception {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode raiz = objectMapper.readTree(respuesta.getBody());

        Set<String> rutas = new HashSet<>();
        raiz.get("paths").fieldNames().forEachRemaining(rutas::add);

        assertThat(rutas).containsExactlyInAnyOrder(
                "/api/v1/accounts/{userId}/balance",
                "/api/v1/accounts/{userId}/movements",
                "/api/v1/funds/deposits",
                "/api/v1/funds/withdrawals",
                "/api/v1/funds/reservations",
                "/api/v1/funds/reservations/{id}",
                "/api/v1/funds/reservations/{id}/confirm",
                "/api/v1/funds/reservations/{id}/release");

        JsonNode seguridad = raiz.at("/components/securitySchemes/bearerAuth");
        assertThat(seguridad.get("type").asText()).isEqualTo("http");
        assertThat(seguridad.get("scheme").asText()).isEqualTo("bearer");
        assertThat(seguridad.get("bearerFormat").asText()).isEqualTo("JWT");
    }
}
