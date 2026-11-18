package co.edu.udistrital.acciones.m6portafolio.application.exception;

import org.springframework.http.HttpStatus;

public class ConflictoEstadoReservaException extends ErrorAplicacion {

    public ConflictoEstadoReservaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }
}
