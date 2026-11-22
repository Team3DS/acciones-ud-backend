package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ErrorAplicacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.NegocioException;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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

    private final EscritorProblemas problemas;

    public GlobalExceptionHandler(EscritorProblemas problemas) {
        this.problemas = problemas;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return responder(HttpStatus.BAD_REQUEST, detalle, request, null);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(@NonNull HttpMessageNotReadableException ex,
                                                                 @NonNull HttpHeaders headers,
                                                                 @NonNull HttpStatusCode status,
                                                                 @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion no es valido", request, null);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(@NonNull ServletRequestBindingException ex,
                                                                          @NonNull HttpHeaders headers,
                                                                          @NonNull HttpStatusCode status,
                                                                          @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(@NonNull TypeMismatchException ex,
                                                        @NonNull HttpHeaders headers,
                                                        @NonNull HttpStatusCode status,
                                                        @NonNull WebRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "Parametro invalido: " + ex.getValue(), request, null);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(@NonNull NoResourceFoundException ex,
                                                                   @NonNull HttpHeaders headers,
                                                                   @NonNull HttpStatusCode status,
                                                                   @NonNull WebRequest request) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado", request, null);
    }

    @ExceptionHandler(ErrorAplicacion.class)
    public ResponseEntity<Object> handleErrorAplicacion(ErrorAplicacion ex, WebRequest request) {
        return responder(ex.estado(), ex.getMessage(), request, ex.codigo());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<Object> handleNegocio(NegocioException ex, WebRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegridad(DataIntegrityViolationException ex, WebRequest request) {
        return responder(HttpStatus.CONFLICT, "Conflicto de datos", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleNoControlado(Exception ex, WebRequest request) {
        log.error("Error no controlado al atender la peticion", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request, null);
    }

    private ResponseEntity<Object> responder(HttpStatusCode estado, String mensaje, WebRequest request,
                                             String codigo) {
        String ruta = request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : "";
        return ResponseEntity.status(estado)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemas.construir(estado, mensaje, ruta, codigo));
    }
}
