package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class FiltroTokenInterno extends OncePerRequestFilter {

    private static final String ESQUEMA = "Bearer ";
    private static final String PREFIJO_AUTORIDAD = "ROLE_";
    private static final String MENSAJE_RECHAZO = "Se requiere el token de servicio interno";

    private final TokenInternoProperties propiedades;
    private final EscritorProblemas problemas;

    public FiltroTokenInterno(TokenInternoProperties propiedades, EscritorProblemas problemas) {
        this.propiedades = propiedades;
        this.problemas = problemas;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return !(ruta.equals("/internal") || ruta.startsWith("/internal/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String esperado = propiedades.token();
        if (!propiedades.configurado()) {
            rechazar(request, response);
            return;
        }
        String recibido = tokenDe(request);
        if (recibido == null || !coincide(recibido, esperado)) {
            rechazar(request, response);
            return;
        }
        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                "servicio-interno", null,
                List.of(new SimpleGrantedAuthority(PREFIJO_AUTORIDAD + UsuarioAutenticado.ROL_SERVICIO_INTERNO)));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);
        cadena.doFilter(request, response);
    }

    private static boolean coincide(String recibido, String esperado) {
        return MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }

    private static String tokenDe(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())) {
            return null;
        }
        String token = cabecera.substring(ESQUEMA.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        SecurityContextHolder.clearContext();
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        problemas.escribir(response, HttpStatus.UNAUTHORIZED, MENSAJE_RECHAZO, request.getRequestURI());
    }
}
