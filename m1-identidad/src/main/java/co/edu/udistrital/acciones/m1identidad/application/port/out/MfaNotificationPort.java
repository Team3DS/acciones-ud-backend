package co.edu.udistrital.acciones.m1identidad.application.port.out;

import java.time.Instant;

/**
 * Entrega al usuario el código MFA generado al iniciar sesión.
 * M1 no envía correos: publica el evento {@code m1.mfa.solicitado} y M8 envía el mensaje.
 */
public interface MfaNotificationPort {

    void codigoSolicitado(Long userId, String correo, String codigo, Instant expiraEn);
}
