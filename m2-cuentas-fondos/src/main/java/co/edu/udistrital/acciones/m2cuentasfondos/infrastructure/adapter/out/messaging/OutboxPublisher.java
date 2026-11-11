package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.messaging;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPublisher implements OutboxPort {

    private final OutboxEventoRepositoryPort outbox;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxPublisher(OutboxEventoRepositoryPort outbox, ObjectMapper objectMapper, Clock clock) {
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void registrar(String aggregateType, String aggregateId, String routingKey, Object payload) {
        OutboxEvento evento = OutboxEvento.crear(UUID.randomUUID(), aggregateType, aggregateId, routingKey,
                serializar(payload, routingKey), MDC.get(CorrelationIdFilter.MDC_KEY), Instant.now(clock));
        outbox.save(evento);
    }

    private String serializar(Object payload, String routingKey) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento " + routingKey, ex);
        }
    }
}
