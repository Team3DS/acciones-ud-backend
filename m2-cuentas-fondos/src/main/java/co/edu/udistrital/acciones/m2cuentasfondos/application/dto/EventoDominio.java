package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EventoDominio(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Object payload) {
}
