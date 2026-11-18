package co.edu.udistrital.acciones.m6portafolio.application.exception;

import org.springframework.http.HttpStatus;

public class SolicitudInvalidaException extends ErrorAplicacion {

    public SolicitudInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, mensaje);
    }
}
