package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class ConflictoIdempotenciaException extends ErrorAplicacion {

    public static final String CODIGO = "CONFLICTO_IDEMPOTENCIA";

    public ConflictoIdempotenciaException(String mensaje) {
        super(HttpStatus.CONFLICT, mensaje, CODIGO);
    }
}
