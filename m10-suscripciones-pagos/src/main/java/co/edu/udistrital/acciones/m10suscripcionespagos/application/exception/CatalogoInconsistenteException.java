package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class CatalogoInconsistenteException extends ErrorAplicacion {

    public static final String CODIGO = "CATALOGO_INCONSISTENTE";

    public CatalogoInconsistenteException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje, CODIGO);
    }
}
