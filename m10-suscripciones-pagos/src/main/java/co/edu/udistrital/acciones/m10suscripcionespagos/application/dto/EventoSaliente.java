package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EventoSaliente(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String source,
        String aggregateId,
        String correlationId,
        Object payload) {
}
