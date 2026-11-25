package co.edu.udistrital.acciones.m1identidad.application.port.out;

import java.time.Instant;

public interface PasswordResetNotificationPort {
    void recuperacionSolicitada(Long userId, String correo, String codigo, Instant expiraEn);
}
