package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleMethodArgumentNotValid() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("orden", "ticker", "no debe estar vacio");
        FieldError error2 = new FieldError("orden", "cantidad", "debe ser mayor a cero");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleMethodArgumentNotValid(ex, null, null, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleIllegalArgument() {
        IllegalArgumentException ex = new IllegalArgumentException("Ticker invalido");

        var response = handler.handleIllegalArgument(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleIllegalState() {
        IllegalStateException ex = new IllegalStateException("Orden ya ejecutada");

        var response = handler.handleIllegalState(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate key");

        var response = handler.handleDataIntegrity(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void handleGeneral() {
        RuntimeException ex = new RuntimeException("Error inesperado");

        var response = handler.handleGeneral(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void responderEstableceContentTypeProblemJson() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("test"), webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody()).isNotNull();
    }
}