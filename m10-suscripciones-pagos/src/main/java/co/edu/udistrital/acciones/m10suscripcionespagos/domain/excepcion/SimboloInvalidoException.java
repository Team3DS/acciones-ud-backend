package co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion;

public class SimboloInvalidoException extends NegocioException {

    public SimboloInvalidoException(String mensaje) {
        super(mensaje);
    }
}
