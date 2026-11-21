package co.edu.udistrital.acciones.m6portafolio.application.exception;

import org.springframework.http.HttpStatus;

public class ConflictoIdempotenciaException extends ErrorAplicacion {

    public ConflictoIdempotenciaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
