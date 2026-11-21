package co.edu.udistrital.acciones.m6portafolio.application.exception;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ErrorAplicacion {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
