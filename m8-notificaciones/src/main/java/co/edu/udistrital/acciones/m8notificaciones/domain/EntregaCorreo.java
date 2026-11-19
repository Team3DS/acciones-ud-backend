package co.edu.udistrital.acciones.m8notificaciones.domain;

import java.time.Instant;
import java.util.UUID;

public record EntregaCorreo(UUID id, String encryptedContent, int attempts, Instant expiresAt) {
    @Override
    public String toString() {
        return "EntregaCorreo[id=" + id + "]";
    }
}
