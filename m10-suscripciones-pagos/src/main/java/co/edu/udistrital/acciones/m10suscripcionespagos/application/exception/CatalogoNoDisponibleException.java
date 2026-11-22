package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

public class CatalogoNoDisponibleException extends ServicioExternoNoDisponibleException {

    public static final String CODIGO = "CATALOGO_NO_DISPONIBLE";

    public CatalogoNoDisponibleException(Throwable causa) {
        super("El catalogo de instrumentos no esta disponible", CODIGO, causa);
    }
}
