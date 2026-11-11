package co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion;

public class CuentaNoOperableException extends NegocioException {

    public CuentaNoOperableException(String mensaje) {
        super(mensaje);
    }
}
