package co.edu.udistrital.acciones.m4datosmercado.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

public final class ApiProblems {
    private ApiProblems() {}

    public static Map<String, Object> create(int status, String type, String detail, HttpServletRequest request) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", type,
                "message", detail,
                "path", request.getRequestURI());
    }
}