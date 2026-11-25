package co.edu.udistrital.acciones.m1identidad.application.port.out;

public interface PasswordResetCompletionPort {
    void register(Long userId, String email);
}
