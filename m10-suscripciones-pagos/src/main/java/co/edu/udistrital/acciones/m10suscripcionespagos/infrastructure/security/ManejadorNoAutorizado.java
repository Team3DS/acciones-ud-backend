package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ManejadorNoAutorizado implements AuthenticationEntryPoint {

    private final EscritorProblemas problemas;

    public ManejadorNoAutorizado(EscritorProblemas problemas) {
        this.problemas = problemas;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        problemas.escribir(response, HttpStatus.UNAUTHORIZED,
                "Se requiere un token JWT valido", request.getRequestURI());
    }
}
