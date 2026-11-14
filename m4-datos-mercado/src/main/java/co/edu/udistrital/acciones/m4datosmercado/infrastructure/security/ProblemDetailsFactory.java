package co.edu.udistrital.acciones.m4datosmercado.infrastructure.security;

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
        return problema;
    }

    private static String descripcion(HttpStatusCode status) {
        org.springframework.http.HttpStatus httpStatus = org.springframework.http.HttpStatus.resolve(status.value());
        return httpStatus == null ? "" : httpStatus.getReasonPhrase();
    }
}
