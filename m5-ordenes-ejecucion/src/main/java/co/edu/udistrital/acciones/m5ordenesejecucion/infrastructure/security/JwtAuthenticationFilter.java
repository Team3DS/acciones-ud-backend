package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ESQUEMA = "Bearer ";
    private static final String PREFIJO_AUTORIDAD = "ROLE_";

    private final JwtTokenValidator validador;

    public JwtAuthenticationFilter(JwtTokenValidator validador) {
        this.validador = validador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            autenticar(request);
        }
        cadena.doFilter(request, response);
    }

    private void autenticar(HttpServletRequest request) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera != null && cabecera.regionMatches(true, 0, ESQUEMA, 0, ESQUEMA.length())) {
            String token = cabecera.substring(ESQUEMA.length()).trim();
            if (!token.isEmpty()) {
                try {
                    AuthenticatedUser usuario = validador.validar(token);
                    var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                            usuario, null, autoridades(usuario));
                    autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    return;
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                }
            }
        }

        // Soporte adicional para cabecera X-User-Id (reenviada desde API Gateway)
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                long userId = Long.parseLong(userIdHeader.trim());
                String rolesHeader = request.getHeader("X-User-Roles");
                Set<String> roles = Set.of("USER");
                if (rolesHeader != null && !rolesHeader.isBlank()) {
                    roles = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                }
                AuthenticatedUser usuario = new AuthenticatedUser(userId, roles);
                var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                        usuario, null, autoridades(usuario));
                autenticacion.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static List<GrantedAuthority> autoridades(AuthenticatedUser usuario) {
        return usuario.roles().stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO_AUTORIDAD + rol))
                .toList();
    }
}
