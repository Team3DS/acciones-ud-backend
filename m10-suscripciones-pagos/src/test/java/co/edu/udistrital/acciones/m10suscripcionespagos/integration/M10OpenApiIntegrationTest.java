package co.edu.udistrital.acciones.m10suscripcionespagos.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class M10OpenApiIntegrationTest extends AbstractIntegrationTest {

    private static final Set<String> RUTAS_ESPERADAS = Set.of(
            "GET /api/v1/payments/plans",
            "GET /api/v1/payments/invoices",
            "POST /api/v1/payments/webhook/stripe",
            "POST /api/v1/subscriptions",
            "GET /api/v1/subscriptions/me",
            "POST /api/v1/subscriptions/{id}/cancel",
            "POST /api/v1/subscriptions/{id}/change-plan",
            "GET /internal/subscriptions/{userId}/entitlement",
            "GET /api/v1/watchlist",
            "POST /api/v1/watchlist/items",
            "DELETE /api/v1/watchlist/items/{symbol}");

    private static final Set<String> METODOS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    private ObjectMapper json;

    @Test
    void elContratoPublicaOnceRutasConSusEsquemasDeSeguridad() throws Exception {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode documento = json.readTree(respuesta.getBody());
        JsonNode rutas = documento.path("paths");

        Set<String> publicadas = new HashSet<>();
        rutas.fieldNames().forEachRemaining(ruta -> METODOS.stream()
                .filter(metodo -> rutas.path(ruta).has(metodo))
                .forEach(metodo -> publicadas.add(metodo.toUpperCase() + " " + ruta)));
        assertThat(publicadas).isEqualTo(RUTAS_ESPERADAS);

        assertThat(esquemas(rutas, "GET", "/api/v1/payments/plans")).isEmpty();
        assertThat(esquemas(rutas, "POST", "/api/v1/payments/webhook/stripe")).isEmpty();
        assertThat(esquemas(rutas, "GET", "/api/v1/subscriptions/me"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "POST", "/api/v1/subscriptions"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "POST", "/api/v1/subscriptions/{id}/cancel"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "POST", "/api/v1/subscriptions/{id}/change-plan"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "GET", "/api/v1/payments/invoices"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "GET", "/api/v1/watchlist"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "POST", "/api/v1/watchlist/items"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "DELETE", "/api/v1/watchlist/items/{symbol}"))
                .containsExactly("bearerAuth");
        assertThat(esquemas(rutas, "GET", "/internal/subscriptions/{userId}/entitlement"))
                .containsExactly("tokenInterno");

        assertThat(documento.path("components").path("securitySchemes").path("bearerAuth").path("scheme").asText())
                .isEqualTo("bearer");
        assertThat(documento.path("components").path("securitySchemes").path("tokenInterno").path("scheme").asText())
                .isEqualTo("bearer");
    }

    @Test
    void losErroresDocumentadosUsanElEsquemaProblemDetail() throws Exception {
        ResponseEntity<String> respuesta = rest.getForEntity("/v3/api-docs", String.class);
        JsonNode rutas = json.readTree(respuesta.getBody()).path("paths");

        List<String> sinEsquema = new ArrayList<>();
        int erroresRevisados = 0;
        Iterator<String> nombresRuta = rutas.fieldNames();
        while (nombresRuta.hasNext()) {
            String ruta = nombresRuta.next();
            JsonNode operaciones = rutas.path(ruta);
            Iterator<String> nombresMetodo = operaciones.fieldNames();
            while (nombresMetodo.hasNext()) {
                String metodo = nombresMetodo.next();
                if (!METODOS.contains(metodo)) {
                    continue;
                }
                JsonNode respuestas = operaciones.path(metodo).path("responses");
                Iterator<String> codigos = respuestas.fieldNames();
                while (codigos.hasNext()) {
                    String codigo = codigos.next();
                    if (!esError(codigo)) {
                        continue;
                    }
                    erroresRevisados++;
                    String contenido = respuestas.path(codigo).path("content").toString();
                    if (!contenido.contains("application/problem+json") || !contenido.contains("ProblemDetail")) {
                        sinEsquema.add(metodo.toUpperCase() + " " + ruta + " -> " + codigo);
                    }
                }
            }
        }

        assertThat(erroresRevisados).isGreaterThanOrEqualTo(10);
        assertThat(sinEsquema).isEmpty();
    }

    private static boolean esError(String codigo) {
        if (codigo.length() != 3 || !codigo.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int valor = Integer.parseInt(codigo);
        return valor >= 400 && valor < 600;
    }

    private static Set<String> esquemas(JsonNode rutas, String metodo, String ruta) {
        JsonNode operacion = rutas.path(ruta).path(metodo.toLowerCase());
        JsonNode lista = operacion.path("security");
        Set<String> esquemas = new HashSet<>();
        lista.forEach(elemento -> elemento.fieldNames().forEachRemaining(esquemas::add));
        return esquemas;
    }
}
