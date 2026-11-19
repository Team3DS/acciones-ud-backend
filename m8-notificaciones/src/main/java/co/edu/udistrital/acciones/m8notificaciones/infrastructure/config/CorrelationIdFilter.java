package co.edu.udistrital.acciones.m8notificaciones.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String supplied = request.getHeader("X-Correlation-Id");
        String id =
                supplied != null && supplied.matches("[A-Za-z0-9._-]{1,100}")
                        ? supplied
                        : UUID.randomUUID().toString();
        response.setHeader("X-Correlation-Id", id);
        try {
            MDC.put("correlationId", id);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
