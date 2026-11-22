package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import java.time.Instant;

public record EntitlementResponse(boolean active, Instant validUntil) {
}
