package co.edu.udistrital.acciones.m10suscripcionespagos.application.exception;

import org.springframework.http.HttpStatus;

public class ProveedorPagosNoDisponibleException extends ErrorAplicacion {

    public static final String CODIGO = "PROVEEDOR_PAGOS_NO_DISPONIBLE";

    public ProveedorPagosNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje, CODIGO);
    }

    public ProveedorPagosNoDisponibleException(String mensaje, Throwable causa) {
        super(HttpStatus.SERVICE_UNAVAILABLE, mensaje, CODIGO, causa);
    }
}
