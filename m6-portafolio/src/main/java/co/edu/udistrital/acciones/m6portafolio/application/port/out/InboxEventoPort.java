package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.InboxEvento;
import java.time.Instant;

public interface InboxEventoPort {

    boolean registrarSiNuevo(String eventId, String eventType, String aggregateId, String correlationId,
                             com.fasterxml.jackson.databind.JsonNode payload, Instant recibidoEn);

    boolean existe(String eventId);
}