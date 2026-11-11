package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.ProblemDetailsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return responder(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                 @NonNull HttpHeaders headers,
                                                                 @NonNull HttpStatusCode status,
                                                                 @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(@NonNull ServletRequestBindingException ex,
                                                                          @NonNull HttpHeaders headers,
                                                                          @NonNull HttpStatusCode status,
                                                                          @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(@NonNull TypeMismatchException ex,
                                                        @NonNull HttpHeaders headers,
                                                        @NonNull HttpStatusCode status,
                                                        @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + ex.getValue(), request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(@NonNull NoResourceFoundException ex,
                                                                   @NonNull HttpHeaders headers,
                                                                   @NonNull HttpStatusCode status,
                                                                   @NonNull WebRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Object> handleAccesoDenegado(AccesoDenegadoException ex, WebRequest request) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleRecursoNoEncontrado(RecursoNoEncontradoException ex, WebRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoIdempotenciaException.class)
    public ResponseEntity<Object> handleConflictoIdempotencia(ConflictoIdempotenciaException ex, WebRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(FondosInsuficientesException.class)
    public ResponseEntity<Object> handleFondosInsuficientes(FondosInsuficientesException ex, WebRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenerico(Exception ex, WebRequest request) {
        log.error("Error no controlado al procesar la petición", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request);
    }

    private ResponseEntity<Object> responder(HttpStatusCode status, String mensaje, WebRequest request) {
        String path = request instanceof ServletWebRequest servlet ? servlet.getRequest().getRequestURI() : null;
        ProblemDetail problema = ProblemDetailsFactory.crear(status, mensaje, path);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }
}
