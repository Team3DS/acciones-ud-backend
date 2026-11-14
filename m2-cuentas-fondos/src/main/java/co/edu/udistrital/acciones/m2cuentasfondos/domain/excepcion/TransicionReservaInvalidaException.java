package co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion;

public class TransicionReservaInvalidaException extends NegocioException {

    public TransicionReservaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
