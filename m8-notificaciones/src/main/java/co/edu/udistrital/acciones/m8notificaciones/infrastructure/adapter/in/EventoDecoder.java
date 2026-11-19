package co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in;

import co.edu.udistrital.acciones.m8notificaciones.application.service.EventosService;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
public class EventoDecoder {
    private final ObjectMapper mapper;

    public EventoDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public EventoAviso decode(byte[] body, String routingKey, String messageId) {
        if (body.length > 65536) {
            throw invalid();
        }
        try {
            JsonNode envelope = mapper.readTree(body);
            if (envelope == null || !envelope.isObject()) {
                throw invalid();
            }
            String type = envelope.path("eventType").asText();
            if (!type.equals(routingKey)) {
                throw invalid();
            }
            String source = EventosService.origen(type);
            if (envelope.has("source") && !source.equals(envelope.path("source").asText())) {
                throw invalid();
            }
            String id = envelope.has("eventId") ? envelope.path("eventId").asText() : messageId;
            if (id == null || !id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                throw invalid();
            }
            JsonNode payload = envelope.path("payload");
            if (payload.isTextual()) {
                payload = mapper.readTree(payload.asText());
            }
            return new EventoAviso(
                    UUID.fromString(id),
                    source,
                    type,
                    Instant.parse(envelope.path("occurredAt").asText()),
                    payload);
        } catch (IOException | DateTimeParseException exception) {
            throw invalid();
        }
    }

    private static AvisoException invalid() {
        return new AvisoException(400, "Formato de evento inválido.");
    }
}
