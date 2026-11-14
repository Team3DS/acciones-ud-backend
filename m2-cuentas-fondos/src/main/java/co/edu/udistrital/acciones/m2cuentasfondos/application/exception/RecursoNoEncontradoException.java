package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
