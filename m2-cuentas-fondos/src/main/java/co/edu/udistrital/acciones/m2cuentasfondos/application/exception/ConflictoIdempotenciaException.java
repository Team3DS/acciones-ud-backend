package co.edu.udistrital.acciones.m2cuentasfondos.application.exception;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;

public class ConflictoIdempotenciaException extends NegocioException {

    public ConflictoIdempotenciaException(String mensaje) {
        super(mensaje);
    }
}
