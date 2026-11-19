package co.edu.udistrital.acciones.m8notificaciones.application.port.out;

public class CorreoException extends RuntimeException {
    private final boolean retryable;

    public CorreoException(String code, boolean retryable) {
        super(code);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
