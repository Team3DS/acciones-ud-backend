package co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion;

public class MontoInvalidoException extends NegocioException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
