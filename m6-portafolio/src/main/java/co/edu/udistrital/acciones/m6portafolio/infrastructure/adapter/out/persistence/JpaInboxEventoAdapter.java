package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.InboxEventoPort;
import co.edu.udistrital.acciones.m6portafolio.domain.InboxEvento;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class JpaInboxEventoAdapter implements InboxEventoPort {

    private final JpaInboxEventoRepository repositorio;

    public JpaInboxEventoAdapter(JpaInboxEventoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public boolean registrarSiNuevo(String eventId, String eventType, String aggregateId, String correlationId,
                                    com.fasterxml.jackson.databind.JsonNode payload, Instant recibidoEn) {
        if (repositorio.existsById(eventId)) {
            return false;
        }
        InboxEvento evento = new InboxEvento(eventId, eventType, aggregateId, correlationId, payload, recibidoEn);
        repositorio.save(evento);
        return true;
    }

    @Override
    public boolean existe(String eventId) {
        return repositorio.existsById(eventId);
    }
}