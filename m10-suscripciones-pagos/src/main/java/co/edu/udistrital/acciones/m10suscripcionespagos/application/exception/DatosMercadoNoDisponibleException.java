package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

public class DatosMercadoNoDisponibleException extends ServicioExternoNoDisponibleException {

    public static final String CODIGO = "DATOS_MERCADO_NO_DISPONIBLE";

    public DatosMercadoNoDisponibleException(Throwable causa) {
        super("Los datos de mercado no estan disponibles", CODIGO, causa);
    }
}
