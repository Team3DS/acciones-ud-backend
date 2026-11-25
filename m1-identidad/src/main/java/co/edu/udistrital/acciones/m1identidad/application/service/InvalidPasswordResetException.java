package co.edu.udistrital.acciones.m1identidad.application.service;

public class InvalidPasswordResetException extends RuntimeException {
    public InvalidPasswordResetException() {
        super("No se pudo restablecer la contraseña. Solicita un nuevo código e inténtalo de nuevo.");
    }
}
