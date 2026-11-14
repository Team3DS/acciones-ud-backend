package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security;

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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtTokenValidator validador;

    public JwtAuthenticationFilter(JwtTokenValidator validador) {
        this.validador = validador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extraerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(request, token);
        }
        filterChain.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request, String token) {
        try {
            AuthenticatedUser usuario = validador.validar(token);
            UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                    usuario, null, autoridades(usuario));
            autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private String extraerToken(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO_BEARER)) {
            return null;
        }
        String token = cabecera.substring(PREFIJO_BEARER.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private List<GrantedAuthority> autoridades(AuthenticatedUser usuario) {
        return usuario.roles().stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
    }
}
