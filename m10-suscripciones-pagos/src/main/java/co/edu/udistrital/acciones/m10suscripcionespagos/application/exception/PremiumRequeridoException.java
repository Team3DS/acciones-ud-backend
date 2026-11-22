package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class PremiumRequeridoException extends ErrorAplicacion {

    public static final String CODIGO = "PREMIUM_REQUERIDO";

    public PremiumRequeridoException() {
        super(HttpStatus.FORBIDDEN, "Se requiere una suscripcion Premium vigente", CODIGO);
    }
}
