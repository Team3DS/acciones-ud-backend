package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPublisher implements OutboxPort {

    private final OutboxEventoRepositoryPort repositorio;
    private final ObjectMapper json;
    private final Clock clock;

    public OutboxPublisher(OutboxEventoRepositoryPort repositorio, ObjectMapper json, Clock clock) {
        this.repositorio = repositorio;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void registrar(String aggregateType, String aggregateId, String eventType, Object payload) {
        String cuerpo = serializar(eventType, payload);
        repositorio.guardar(OutboxEvento.registrar(UUID.randomUUID(), aggregateType, aggregateId,
                eventType, cuerpo, MDC.get(CorrelationIdFilter.CLAVE_MDC), Instant.now(clock)));
    }

    private String serializar(String eventType, Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento " + eventType, ex);
        }
    }
}
