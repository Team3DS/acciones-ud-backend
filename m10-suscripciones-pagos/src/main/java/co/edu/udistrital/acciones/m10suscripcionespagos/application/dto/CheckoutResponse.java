package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;

import java.time.Instant;

public record CheckoutResponse(
        String sessionId,
        String checkoutUrl,
        Instant expiresAt) {

    public static CheckoutResponse de(SesionCheckout sesion) {
        return new CheckoutResponse(sesion.getStripeSessionId(), sesion.getUrl(), sesion.getExpiraEn());
    }
}
