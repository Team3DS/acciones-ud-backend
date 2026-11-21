package co.edu.udistrital.acciones.m6portafolio.domain.excepcion;

public class VentaNoDisponibleException extends NegocioException {

    public static final String CODIGO = "VENTA_NO_DISPONIBLE";

    public VentaNoDisponibleException(String mensaje) {
        super(CODIGO + ": " + mensaje);
    }
}
