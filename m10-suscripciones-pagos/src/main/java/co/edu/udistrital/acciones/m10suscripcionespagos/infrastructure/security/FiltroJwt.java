package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FiltroJwt extends OncePerRequestFilter {

    private static final String ESQUEMA = "Bearer ";
    private static final String PREFIJO_AUTORIDAD = "ROLE_";

    private final VerificadorJwt verificador;

    public FiltroJwt(VerificadorJwt verificador) {
        this.verificador = verificador;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return ruta.equals("/internal") || ruta.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticarSiHayToken(request);
        }
        cadena.doFilter(request, response);
    }

    private void autenticarSiHayToken(HttpServletRequest request) {
        String token = tokenDe(request);
        if (token == null) {
            return;
        }
        try {
            UsuarioAutenticado usuario = verificador.validar(token);
            var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                    usuario, null, autoridades(usuario));
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private static String tokenDe(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())) {
            return null;
        }
        String token = cabecera.substring(ESQUEMA.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static List<GrantedAuthority> autoridades(UsuarioAutenticado usuario) {
        return usuario.roles().stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO_AUTORIDAD + rol))
                .toList();
    }
}
