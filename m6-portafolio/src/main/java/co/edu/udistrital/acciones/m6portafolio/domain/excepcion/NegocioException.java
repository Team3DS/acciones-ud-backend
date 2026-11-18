package co.edu.udistrital.acciones.m6portafolio.domain.excepcion;

public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
