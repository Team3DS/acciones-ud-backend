package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import java.time.Instant;

public record DatosSuscripcion(
        String stripeSubscriptionId,
        PrecioPlan precio,
        EstadoSuscripcion estado,
        Instant periodoInicio,
        Instant periodoFin,
        boolean cancelarAlFinal,
        Instant canceladaEn,
        Instant finalizadaEn) {
}
