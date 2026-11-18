package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ApiProblemWriter;
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

class ApiProblemWriterTest {

    private static final Instant AHORA = Instant.parse("2026-01-02T03:04:05Z");

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ApiProblemWriter writer =
            new ApiProblemWriter(objectMapper, Clock.fixed(AHORA, ZoneOffset.UTC));

    @AfterEach
    void limpiarContexto() {
        MDC.clear();
    }

    @Test
    void construyeElProblemDetailConTodosLosCamposDelContrato() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-123");

        ProblemDetail problema = writer.construir(
                HttpStatus.NOT_FOUND, "La posicion no existe", "/api/v1/positions/1/AAPL");

        assertThat(problema.getStatus()).isEqualTo(404);
        assertThat(problema.getTitle()).isEqualTo("Not Found");
        assertThat(problema.getDetail()).isEqualTo("La posicion no existe");
        assertThat(problema.getInstance()).hasToString("/api/v1/positions/1/AAPL");
        assertThat(problema.getProperties())
                .containsEntry("timestamp", AHORA)
                .containsEntry("error", "Not Found")
                .containsEntry("message", "La posicion no existe")
                .containsEntry("path", "/api/v1/positions/1/AAPL")
                .containsEntry("traceId", "traza-123");
    }

    @Test
    void dejaElTraceIdVacioCuandoNoHayCorrelationId() {
        ProblemDetail problema = writer.construir(HttpStatus.CONFLICT, "Clave repetida", "/ruta");

        assertThat(problema.getProperties()).containsEntry("traceId", "");
    }

    @Test
    void admiteRutasNulasOVacias() {
        ProblemDetail sinRuta = writer.construir(HttpStatus.BAD_REQUEST, "Dato invalido", null);
        ProblemDetail rutaVacia = writer.construir(HttpStatus.BAD_REQUEST, "Dato invalido", " ");

        assertThat(sinRuta.getInstance()).isNull();
        assertThat(sinRuta.getProperties()).containsEntry("path", "");
        assertThat(rutaVacia.getInstance()).isNull();
    }

    @Test
    void escribeLaRespuestaComoProblemJson() throws Exception {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-escritura");
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.escribir(response, HttpStatus.UNAUTHORIZED, "Se requiere un token JWT valido", "/api/v1/portafolio/1");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> cuerpo = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertThat(cuerpo)
                .containsEntry("status", 401)
                .containsEntry("message", "Se requiere un token JWT valido")
                .containsEntry("path", "/api/v1/portafolio/1")
                .containsEntry("traceId", "traza-escritura");
    }
}
