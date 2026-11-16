package co.edu.udistrital.acciones.m3catalogomercados.domain;

import java.time.Instant;
import java.util.UUID;

public record EventoCatalogo(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Object payload) {}
