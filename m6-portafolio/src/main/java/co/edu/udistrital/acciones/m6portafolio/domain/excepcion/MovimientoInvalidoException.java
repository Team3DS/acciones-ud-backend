package co.edu.udistrital.acciones.m6portafolio.domain.excepcion;

public class MovimientoInvalidoException extends NegocioException {

    public MovimientoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
