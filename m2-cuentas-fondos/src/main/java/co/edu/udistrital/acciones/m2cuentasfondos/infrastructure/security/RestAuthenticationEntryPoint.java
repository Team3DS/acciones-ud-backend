package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailsWriter writer;

    public RestAuthenticationEntryPoint(ProblemDetailsWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        writer.escribir(response, HttpStatus.UNAUTHORIZED,
                "Se requiere un token JWT válido", request.getRequestURI());
    }
}
