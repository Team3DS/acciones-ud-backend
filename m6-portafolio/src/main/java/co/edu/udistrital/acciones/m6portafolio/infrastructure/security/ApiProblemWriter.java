package co.edu.udistrital.acciones.m6portafolio.infrastructure.security;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Component
public class ApiProblemWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiProblemWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public ProblemDetail construir(HttpStatusCode estado, String mensaje, String ruta) {
        String titulo = titulo(estado);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, mensaje);
        problema.setTitle(titulo);
        if (ruta != null && !ruta.isBlank()) {
            problema.setInstance(URI.create(ruta));
        }
        problema.setProperty("timestamp", clock.instant());
        problema.setProperty("error", titulo);
        problema.setProperty("message", mensaje);
        problema.setProperty("path", ruta == null ? "" : ruta);
        problema.setProperty("traceId", traceId());
        return problema;
    }

    public void escribir(HttpServletResponse response, HttpStatus estado, String mensaje, String ruta)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), construir(estado, mensaje, ruta));
    }

    private static String titulo(HttpStatusCode estado) {
        HttpStatus resuelto = HttpStatus.resolve(estado.value());
        return resuelto == null ? "" : resuelto.getReasonPhrase();
    }

    private static String traceId() {
        String correlationId = MDC.get(CorrelationIdFilter.CLAVE_MDC);
        return correlationId == null ? "" : correlationId;
    }
}
