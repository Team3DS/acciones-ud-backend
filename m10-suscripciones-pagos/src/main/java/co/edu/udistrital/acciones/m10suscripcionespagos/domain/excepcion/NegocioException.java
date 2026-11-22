package co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion;

public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
