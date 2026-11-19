package co.edu.udistrital.acciones.m8notificaciones.domain;

public class AvisoException extends RuntimeException {
    private final int status;

    public AvisoException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
