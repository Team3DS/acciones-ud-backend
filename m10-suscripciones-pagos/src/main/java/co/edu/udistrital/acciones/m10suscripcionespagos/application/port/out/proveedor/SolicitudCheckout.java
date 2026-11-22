package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor;

import java.time.Instant;
import java.util.Objects;

public record SolicitudCheckout(
        Long userId,
        String stripePriceId,
        Instant expiraEn,
        String successUrl,
        String cancelUrl,
        String customerId,
        String idempotencyKey) {

    public SolicitudCheckout {
        Objects.requireNonNull(userId, "El usuario es obligatorio");
        Objects.requireNonNull(stripePriceId, "El precio de Stripe es obligatorio");
        Objects.requireNonNull(expiraEn, "La expiracion es obligatoria");
        Objects.requireNonNull(successUrl, "La url de exito es obligatoria");
        Objects.requireNonNull(cancelUrl, "La url de cancelacion es obligatoria");
        Objects.requireNonNull(idempotencyKey, "La clave de idempotencia es obligatoria");
    }
}
