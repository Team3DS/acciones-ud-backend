package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;

import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class AvisosExceptionHandler {
    @ExceptionHandler(AvisoException.class)
    public ResponseEntity<ProblemDetail> domain(AvisoException exception) {
        return problem(exception.status(), exception.getMessage());
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MissingRequestHeaderException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> invalid(Exception exception) {
        return problem(400, "Solicitud inválida.");
    }

    private static ResponseEntity<ProblemDetail> problem(int status, String detail) {
        ProblemDetail body =
                ProblemDetail.forStatusAndDetail(
                        org.springframework.http.HttpStatusCode.valueOf(status), detail);
        body.setProperty("correlationId", MDC.get("correlationId"));
        return ResponseEntity.status(status).body(body);
    }
}
