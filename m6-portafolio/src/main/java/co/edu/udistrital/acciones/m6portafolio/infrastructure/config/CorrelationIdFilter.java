package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-ID";
    public static final String CLAVE_MDC = "correlationId";
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final int LONGITUD_MAXIMA = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolver(request.getHeader(HEADER));
        response.setHeader(HEADER, correlationId);
        try (var contexto = MDC.putCloseable(CLAVE_MDC, correlationId)) {
            chain.doFilter(request, response);
            LOG.info("Solicitud procesada");
        }
    }

    private String resolver(String recibido) {
        return esAceptable(recibido) ? recibido : UUID.randomUUID().toString();
    }

    private boolean esAceptable(String valor) {
        if (valor == null || valor.isBlank() || valor.length() > LONGITUD_MAXIMA) {
            return false;
        }
        return valor.chars().allMatch(CorrelationIdFilter::esCaracterSeguro);
    }

    private static boolean esCaracterSeguro(int caracter) {
        return (caracter >= 'a' && caracter <= 'z')
                || (caracter >= 'A' && caracter <= 'Z')
                || (caracter >= '0' && caracter <= '9')
                || caracter == '-'
                || caracter == '_';
    }
}
