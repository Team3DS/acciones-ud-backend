package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class AccesoDenegadoException extends NegocioException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
