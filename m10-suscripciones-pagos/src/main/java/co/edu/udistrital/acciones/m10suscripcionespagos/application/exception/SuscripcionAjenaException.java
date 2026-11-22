package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class SuscripcionAjenaException extends ErrorAplicacion {

    public SuscripcionAjenaException(String mensaje) {
        super(HttpStatus.FORBIDDEN, mensaje);
    }
}
