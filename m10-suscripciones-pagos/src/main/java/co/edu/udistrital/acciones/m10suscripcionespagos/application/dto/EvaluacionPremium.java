package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import java.time.Instant;

public record EvaluacionPremium(boolean premium, Instant validUntil) {
}
