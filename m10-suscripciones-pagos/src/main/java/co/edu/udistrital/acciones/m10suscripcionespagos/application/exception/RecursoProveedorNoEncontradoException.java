package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class RecursoProveedorNoEncontradoException extends ErrorAplicacion {

    public RecursoProveedorNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, mensaje);
    }
}
