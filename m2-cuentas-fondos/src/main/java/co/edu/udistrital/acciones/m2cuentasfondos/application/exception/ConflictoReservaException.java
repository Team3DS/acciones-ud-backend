package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class ConflictoReservaException extends NegocioException {

    public ConflictoReservaException(String mensaje) {
        super(mensaje);
    }
}
