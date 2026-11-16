package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    HttpServletRequest request;

    @Mock
    BindingResult bindingResult;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ObjectMapper());

    @Test
    void handleSimboloInvalido() {
        when(request.getRequestURI()).thenReturn("/api/v1/quotes/AAPL");
        SimboloInvalidoException ex = new SimboloInvalidoException("INVALID");

        ResponseEntity<Map<String, Object>> response = handler.handleSimboloInvalido(ex, request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("SIMBOLO_INVALIDO", response.getBody().get("error"));
        assertTrue(response.getBody().get("message").toString().contains("INVALID"));
    }

    @Test
    void handleResponseStatus() {
        when(request.getRequestURI()).thenReturn("/api/test");
        ResponseStatusException ex = new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Bad request");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatus(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("ERROR", response.getBody().get("error"));
    }

    @Test
    void handleValidation() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/test");

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, request);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void handleGeneric() {
        when(request.getRequestURI()).thenReturn("/api/test");
        Exception ex = new RuntimeException("Internal error");

        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("ERROR_INTERNO", response.getBody().get("error"));
    }

}