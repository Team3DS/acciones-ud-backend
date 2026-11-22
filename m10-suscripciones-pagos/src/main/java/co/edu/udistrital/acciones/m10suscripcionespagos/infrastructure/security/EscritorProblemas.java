package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EscritorProblemas {

    private final ObjectMapper objectMapper;
    private final Clock reloj;

    public EscritorProblemas(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.reloj = clock;
    }

    public ProblemDetail construir(HttpStatusCode estado, String mensaje, String ruta) {
        return construir(estado, mensaje, ruta, null);
    }

    public ProblemDetail construir(HttpStatusCode estado, String mensaje, String ruta, String codigo) {
        String titulo = titulo(estado);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, mensaje);
        problema.setTitle(titulo);
        if (ruta != null && !ruta.isBlank()) {
            problema.setInstance(URI.create(ruta));
        }
        camposComunes(estado, mensaje, ruta, titulo).forEach(problema::setProperty);
        if (codigo != null && !codigo.isBlank()) {
            problema.setProperty("code", codigo);
        }
        return problema;
    }

    public void escribir(HttpServletResponse response, HttpStatus estado, String mensaje, String ruta)
            throws IOException {
        escribir(response, estado, mensaje, ruta, null);
    }

    public void escribir(HttpServletResponse response, HttpStatus estado, String mensaje, String ruta, String codigo)
            throws IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), construir(estado, mensaje, ruta, codigo));
    }

    private Map<String, Object> camposComunes(HttpStatusCode estado, String mensaje, String ruta, String titulo) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("timestamp", reloj.instant());
        campos.put("error", titulo);
        campos.put("message", mensaje);
        campos.put("path", ruta == null ? "" : ruta);
        campos.put("traceId", traza());
        campos.put("status", estado.value());
        return campos;
    }

    private static String titulo(HttpStatusCode estado) {
        HttpStatus resuelto = HttpStatus.resolve(estado.value());
        return resuelto == null ? "" : resuelto.getReasonPhrase();
    }

    private static String traza() {
        String correlationId = MDC.get(CorrelationIdFilter.CLAVE_MDC);
        return correlationId == null ? "" : correlationId;
    }
}
