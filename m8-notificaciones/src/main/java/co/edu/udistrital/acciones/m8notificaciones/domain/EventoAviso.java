package co.edu.udistrital.acciones.m8notificaciones.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record EventoAviso(
        UUID eventId, String source, String eventType, Instant occurredAt, JsonNode payload) {
    @Override
    public String toString() {
        return "EventoAviso[id=" + eventId + ", source=" + source + "]";
    }
}
