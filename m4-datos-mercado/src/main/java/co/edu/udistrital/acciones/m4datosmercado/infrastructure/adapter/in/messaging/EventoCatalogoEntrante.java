package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record EventoCatalogoEntrante(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        Map<String, Object> payload) {}