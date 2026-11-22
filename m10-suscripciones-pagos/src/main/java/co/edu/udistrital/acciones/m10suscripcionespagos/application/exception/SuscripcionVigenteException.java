package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

public class SuscripcionVigenteException extends ConflictoEstadoException {

    public static final String CODIGO = "SUSCRIPCION_VIGENTE";

    public SuscripcionVigenteException(String mensaje) {
        super(mensaje, CODIGO);
    }
}
