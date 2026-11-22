package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EscritorProblemasTest {

    private static final Instant AHORA = Instant.parse("2026-01-02T03:04:05Z");

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final EscritorProblemas escritor =
            new EscritorProblemas(objectMapper, Clock.fixed(AHORA, ZoneOffset.UTC));

    @AfterEach
    void limpiarContexto() {
        MDC.clear();
    }

    @Test
    void construyeElProblemDetailConTodosLosCamposDelContrato() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-123");

        ProblemDetail problema = escritor.construir(
                HttpStatus.NOT_FOUND, "La suscripcion no existe", "/api/v1/subscriptions/1");

        assertThat(problema.getStatus()).isEqualTo(404);
        assertThat(problema.getTitle()).isEqualTo("Not Found");
        assertThat(problema.getDetail()).isEqualTo("La suscripcion no existe");
        assertThat(problema.getInstance()).hasToString("/api/v1/subscriptions/1");
        assertThat(problema.getProperties())
                .containsEntry("timestamp", AHORA)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "La suscripcion no existe")
                .containsEntry("path", "/api/v1/subscriptions/1")
                .containsEntry("traceId", "traza-123")
                .doesNotContainKey("code");
    }

    @Test
    void agregaLaPropiedadCodeCuandoSeInforma() {
        ProblemDetail problema = escritor.construir(
                HttpStatus.FORBIDDEN, "Se requiere Premium", "/api/v1/watchlist", "PREMIUM_REQUERIDO");

        assertThat(problema.getProperties()).containsEntry("code", "PREMIUM_REQUERIDO");
    }

    @Test
    void omiteLaPropiedadCodeCuandoEsNulaOVacia() {
        ProblemDetail nula = escritor.construir(HttpStatus.BAD_REQUEST, "Dato invalido", "/ruta", null);
        ProblemDetail vacia = escritor.construir(HttpStatus.BAD_REQUEST, "Dato invalido", "/ruta", " ");

        assertThat(nula.getProperties()).doesNotContainKey("code");
        assertThat(vacia.getProperties()).doesNotContainKey("code");
    }

    @Test
    void dejaElTraceIdVacioCuandoNoHayCorrelationId() {
        ProblemDetail problema = escritor.construir(HttpStatus.CONFLICT, "Clave repetida", "/ruta");

        assertThat(problema.getProperties()).containsEntry("traceId", "");
    }

    @Test
    void admiteRutasNulasOVacias() {
        ProblemDetail sinRuta = escritor.construir(HttpStatus.BAD_REQUEST, "Dato invalido", null);
        ProblemDetail rutaVacia = escritor.construir(HttpStatus.BAD_REQUEST, "Dato invalido", " ");

        assertThat(sinRuta.getInstance()).isNull();
        assertThat(sinRuta.getProperties()).containsEntry("path", "");
        assertThat(rutaVacia.getInstance()).isNull();
    }

    @Test
    void escribeLaRespuestaComoProblemJson() throws Exception {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-escritura");
        MockHttpServletResponse response = new MockHttpServletResponse();

        escritor.escribir(response, HttpStatus.UNAUTHORIZED, "Se requiere un token JWT valido",
                "/api/v1/subscriptions/me");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> cuerpo = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertThat(cuerpo)
                .containsEntry("status", 401)
                .containsEntry("message", "Se requiere un token JWT valido")
                .containsEntry("path", "/api/v1/subscriptions/me")
                .containsEntry("traceId", "traza-escritura");
    }
}
