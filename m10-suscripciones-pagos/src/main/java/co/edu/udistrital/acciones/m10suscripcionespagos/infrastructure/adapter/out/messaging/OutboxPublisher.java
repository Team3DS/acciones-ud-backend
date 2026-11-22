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

    private final ObjectMapper json;
    private final Clock reloj;
    private final OutboxEventoRepositoryPort almacen;

    public OutboxPublisher(OutboxEventoRepositoryPort repositorio, ObjectMapper json, Clock clock) {
        this.json = json;
        this.reloj = clock;
        this.almacen = repositorio;
    }

    @Override
    public void registrar(String aggregateType, String aggregateId, String eventType, Object payload) {
        almacen.guardar(nuevoRegistro(aggregateType, aggregateId, eventType, payload));
    }

    private OutboxEvento nuevoRegistro(String aggregateType, String aggregateId,
                                       String eventType, Object payload) {
        return OutboxEvento.pendiente(UUID.randomUUID(), aggregateType, aggregateId, eventType,
                convertir(payload, eventType), MDC.get(CorrelationIdFilter.CLAVE_MDC), Instant.now(reloj));
    }

    private String convertir(Object payload, String eventType) {
        try {
            return json.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar el evento " + eventType, ex);
        }
    }
}
