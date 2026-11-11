package co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion;

public class FondosInsuficientesException extends NegocioException {

    public FondosInsuficientesException(String mensaje) {
        super(mensaje);
    }
}
