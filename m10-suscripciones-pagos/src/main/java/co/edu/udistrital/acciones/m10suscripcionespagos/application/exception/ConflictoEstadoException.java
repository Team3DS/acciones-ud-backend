package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class ConflictoEstadoException extends ErrorAplicacion {

    public ConflictoEstadoException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje);
    }

    public ConflictoEstadoException(String mensaje, String codigo) {
        super(HttpStatus.CONFLICT, mensaje, codigo);
    }
}
