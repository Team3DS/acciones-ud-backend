package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEntrante(
        String eventId,
        String eventType,
        Instant occurredAt,
        String aggregateId,
        String correlationId,
        JsonNode payload) {
}
