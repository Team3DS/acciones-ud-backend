package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record EventoEntrante(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("aggregateId") String aggregateId,
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("payload") Object payload) {

    public EventoEntrante {
        if (eventId == null || eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventId y eventType son obligatorios");
        }
    }
}