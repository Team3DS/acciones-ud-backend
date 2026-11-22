package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String CLAVE_MDC = "correlationId";

    private static final Pattern FORMATO_VALIDO = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = normalizar(request.getHeader(HEADER));
        response.setHeader(HEADER, correlationId);
        MDC.put(CLAVE_MDC, correlationId);
        try {
            chain.doFilter(request, response);
            LOG.info("Peticion atendida");
        } finally {
            MDC.remove(CLAVE_MDC);
        }
    }

    private String normalizar(String recibido) {
        if (recibido != null && FORMATO_VALIDO.matcher(recibido).matches()) {
            return recibido;
        }
        return UUID.randomUUID().toString();
    }
}
