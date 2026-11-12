package co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion;

public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
