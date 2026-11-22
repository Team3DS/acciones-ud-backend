package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload;

import java.time.Instant;

public record SuscripcionActivadaPayload(
        Long userId,
        String planId,
        String interval,
        String stripeSubscriptionId,
        Instant validUntil) {
}
