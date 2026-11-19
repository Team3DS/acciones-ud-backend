package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.CorrelationIdFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    FilterChain chain;

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generaCorrelationIdSiNoExiste() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(eq(CorrelationIdFilter.HEADER_NAME), anyString());
        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void usaCorrelationIdExistente() throws ServletException, IOException {
        String existingId = "existing-correlation-id";
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn(existingId);

        filter.doFilter(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.HEADER_NAME, existingId);
        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void limpiaMdcDespuesDeExcepcion() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER_NAME)).thenReturn("test-id");
        doThrow(new RuntimeException("test")).when(chain).doFilter(request, response);

        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}