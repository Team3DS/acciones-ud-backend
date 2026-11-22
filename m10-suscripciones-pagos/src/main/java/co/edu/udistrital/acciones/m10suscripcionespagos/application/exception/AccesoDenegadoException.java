package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class AccesoDenegadoException extends ErrorAplicacion {

    public AccesoDenegadoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
