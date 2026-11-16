package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.config.ApiProblems;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenValidator validator;
    private final ObjectMapper mapper;

    public JwtAuthenticationFilter(JwtTokenValidator validator, ObjectMapper mapper) {
        this.validator = validator;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/actuator/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && !header.isBlank()) {
            try {
                if (!header.startsWith("Bearer "))
                    throw new JwtException("Autenticación inválida.");
                UsuarioAutenticado user = validator.autenticar(header.substring(7));
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        user.id(),
                                        null,
                                        user.roles().stream()
                                                .map(
                                                        org.springframework.security.core.authority
                                                                        .SimpleGrantedAuthority
                                                                ::new)
                                                .toList()));
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(401);
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                mapper.writeValue(
                        response.getOutputStream(),
                        ApiProblems.create(
                                401,
                                "TOKEN_INVALIDO",
                                "Se requiere un token válido y vigente.",
                                request));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
