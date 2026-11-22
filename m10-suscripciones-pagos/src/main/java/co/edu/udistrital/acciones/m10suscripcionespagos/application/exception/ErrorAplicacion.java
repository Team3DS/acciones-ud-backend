package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public abstract class ErrorAplicacion extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;

    protected ErrorAplicacion(HttpStatus estado, String mensaje) {
        this(estado, mensaje, null);
    }

    protected ErrorAplicacion(HttpStatus estado, String mensaje, String codigo) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
    }

    protected ErrorAplicacion(HttpStatus estado, String mensaje, String codigo, Throwable causa) {
        super(mensaje, causa);
        this.estado = estado;
        this.codigo = codigo;
    }

    public HttpStatus estado() {
        return estado;
    }

    public String codigo() {
        return codigo;
    }
}
