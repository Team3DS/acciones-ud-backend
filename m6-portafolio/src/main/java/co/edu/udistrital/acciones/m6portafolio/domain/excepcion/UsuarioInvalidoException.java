package co.edu.udistrital.acciones.m6portafolio.domain.excepcion;

public class UsuarioInvalidoException extends NegocioException {

    public UsuarioInvalidoException(String mensaje) {
        super(mensaje);
    }
}
