package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import java.time.Instant;

public record MiSuscripcionResponse(
        Long userId,
        boolean premium,
        Instant validUntil,
        SuscripcionResponse subscription) {
}
