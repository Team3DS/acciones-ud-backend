package co.edu.udistrital.acciones.m6portafolio.application.exception;

import org.springframework.http.HttpStatus;

public abstract class ErrorAplicacion extends RuntimeException {

    private final HttpStatus estado;

    protected ErrorAplicacion(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus estado() {
        return estado;
    }
}
