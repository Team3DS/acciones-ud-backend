package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

public final class ProblemDetailsFactory {

    private ProblemDetailsFactory() {
    }

    public static ProblemDetail crear(HttpStatusCode status, String mensaje, String path) {
        String error = descripcion(status);
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensaje);
        problema.setTitle(error);
        if (path != null && !path.isBlank()) {
            problema.setInstance(URI.create(path));
        }
        problema.setProperty("timestamp", Instant.now());
        problema.setProperty("error", error);
        problema.setProperty("message", mensaje);
        problema.setProperty("path", path == null ? "" : path);
        problema.setProperty("traceId", traceId());
        return problema;
    }

    private static String descripcion(HttpStatusCode status) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return httpStatus == null ? "" : httpStatus.getReasonPhrase();
    }

    private static String traceId() {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        return correlationId == null ? "" : correlationId;
    }
}
