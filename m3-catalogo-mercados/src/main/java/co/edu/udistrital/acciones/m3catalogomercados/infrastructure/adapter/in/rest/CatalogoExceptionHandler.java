package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.rest;

import co.edu.udistrital.acciones.m3catalogomercados.domain.CatalogoException;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.config.ApiProblems;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class CatalogoExceptionHandler {
    @ExceptionHandler(CatalogoException.class)
    public ResponseEntity<ProblemDetail> catalogo(
            CatalogoException exception, HttpServletRequest request) {
        return problem(exception.status(), exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> argumento(
            IllegalArgumentException exception, HttpServletRequest request) {
        return problem(400, "PARAMETRO_INVALIDO", exception.getMessage(), request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        org.springframework.web.bind.MissingRequestHeaderException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> solicitud(
            Exception exception, HttpServletRequest request) {
        return problem(
                400,
                "SOLICITUD_INVALIDA",
                "Revise el formato y los límites de los parámetros de la solicitud.",
                request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflicto(
            org.springframework.dao.DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return problem(
                409,
                "CATALOGO_CONFLICTO",
                "El registro ya existe o viola las restricciones del catálogo.",
                request);
    }

    private ResponseEntity<ProblemDetail> problem(
            int status, String code, String detail, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiProblems.create(status, code, detail, request));
    }
}
