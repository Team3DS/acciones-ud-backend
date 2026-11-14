package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.ProcesarEventoEntranteService;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.scheduling.OutboxRelay;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class FondosMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(FondosMessageConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcesarEventoEntranteService procesarEvento;

    public FondosMessageConsumer(ObjectMapper objectMapper, ProcesarEventoEntranteService procesarEvento) {
        this.objectMapper = objectMapper;
        this.procesarEvento = procesarEvento;
    }

    @RabbitListener(queues = RoutingKeys.COLA_MOVIMIENTOS, containerFactory = "fondosListenerContainerFactory")
    public void recibir(Message mensaje) {
        EventoEntrante evento = parsear(mensaje);
        if (evento.eventId() == null || evento.eventId().isBlank()
                || evento.eventType() == null || evento.eventType().isBlank()) {
            throw new AmqpRejectAndDontRequeueException(
                    "El sobre del evento no trae eventId o eventType; se rechaza sin reencolar");
        }

        String correlacion = evento.correlationId();
        if (correlacion == null || correlacion.isBlank()) {
            Object cabecera = mensaje.getMessageProperties().getHeaders().get(OutboxRelay.CABECERA_CORRELACION);
            correlacion = cabecera == null ? null : cabecera.toString();
        }

        String correlacionPrevia = MDC.get(CorrelationIdFilter.MDC_KEY);
        try {
            if (correlacion != null && !correlacion.isBlank()) {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlacion);
            }
            procesarEvento.procesar(evento);
        } finally {
            if (correlacionPrevia == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlacionPrevia);
            }
        }
    }

    private EventoEntrante parsear(Message mensaje) {
        try {
            EventoEntrante evento = objectMapper.readValue(mensaje.getBody(), EventoEntrante.class);
            if (evento == null) {
                throw new AmqpRejectAndDontRequeueException("El cuerpo del mensaje está vacío");
            }
            return evento;
        } catch (IOException ex) {
            log.warn("Mensaje inválido recibido en la cola {}; se rechaza sin reencolar",
                    RoutingKeys.COLA_MOVIMIENTOS, ex);
            throw new AmqpRejectAndDontRequeueException(
                    "JSON inválido en la cola " + RoutingKeys.COLA_MOVIMIENTOS, ex);
        }
    }
}
