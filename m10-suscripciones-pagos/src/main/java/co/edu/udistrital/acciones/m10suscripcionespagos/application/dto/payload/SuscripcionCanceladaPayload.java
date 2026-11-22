package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload;

import java.time.Instant;

public record SuscripcionCanceladaPayload(
        Long userId,
        String planId,
        Instant canceledAt) {
}
