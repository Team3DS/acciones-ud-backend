package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class OrdenInvalidoException extends NegocioException {

    public OrdenInvalidoException(String mensaje) {
        super(mensaje);
    }
}
