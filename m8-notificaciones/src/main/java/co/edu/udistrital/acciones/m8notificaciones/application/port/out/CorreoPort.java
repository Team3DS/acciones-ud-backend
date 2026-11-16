package co.edu.udistrital.acciones.m8notificaciones.application.port.out;

import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;

import java.util.UUID;

public interface CorreoPort {
    String enviar(UUID id, Correo correo);
}
