package co.edu.udistrital.acciones.m6portafolio.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ForbiddenAccessHandler implements AccessDeniedHandler {

    private final ApiProblemWriter writer;

    public ForbiddenAccessHandler(ApiProblemWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        writer.escribir(response, HttpStatus.FORBIDDEN,
                "No tiene permisos para realizar esta operacion", request.getRequestURI());
    }
}
