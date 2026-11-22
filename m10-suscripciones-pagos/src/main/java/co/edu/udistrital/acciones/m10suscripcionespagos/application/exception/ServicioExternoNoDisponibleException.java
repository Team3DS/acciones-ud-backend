package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class ServicioExternoNoDisponibleException extends ErrorAplicacion {

    public ServicioExternoNoDisponibleException(String mensaje, String codigo) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje, codigo);
    }
}
