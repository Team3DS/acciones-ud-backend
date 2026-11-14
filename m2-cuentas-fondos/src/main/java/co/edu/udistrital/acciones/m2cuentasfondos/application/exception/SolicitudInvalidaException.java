package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class SolicitudInvalidaException extends NegocioException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
