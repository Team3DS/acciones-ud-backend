package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload;

import java.time.Instant;

public record PagoFallidoPayload(
        Long userId,
        Long invoiceId,
        int attempt,
        Instant nextRetry) {
}
