package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

public final class ApiProblems {
    private ApiProblems() {}

    public static ProblemDetail create(
            int status, String code, String detail, HttpServletRequest request) {
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problem.setTitle(httpStatus.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("error", httpStatus.getReasonPhrase());
        problem.setProperty("message", detail);
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("traceId", MDC.get("traceId"));
        return problem;
    }
}
