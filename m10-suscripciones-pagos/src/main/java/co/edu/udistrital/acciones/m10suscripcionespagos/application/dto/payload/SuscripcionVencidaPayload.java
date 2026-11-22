package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload;

import java.time.Instant;

public record SuscripcionVencidaPayload(
        Long userId,
        String planId,
        Instant expiredAt,
        String reason) {
}
