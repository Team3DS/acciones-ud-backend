package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class SimboloNoDisponibleException extends ErrorAplicacion {

    public static final String CODIGO = "SIMBOLO_NO_DISPONIBLE";

    public SimboloNoDisponibleException(String symbol) {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
                "El simbolo " + symbol + " no esta disponible en el catalogo", CODIGO);
    }
}
