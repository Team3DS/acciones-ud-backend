package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m4datosmercado.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.InstrumentoCreadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.InstrumentoActualizadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.MercadoCreadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.MercadoActualizadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m4datosmercado.application.service.CatalogoEventService;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.CorrelationIdFilter;

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
public class CatalogoEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogoEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final CatalogoEventService catalogoEventService;

    public CatalogoEventConsumer(ObjectMapper objectMapper, CatalogoEventService catalogoEventService) {
        this.objectMapper = objectMapper;
        this.catalogoEventService = catalogoEventService;
    }

    @RabbitListener(queues = RoutingKeys.COLA_M4_EVENTOS, containerFactory = "m4ListenerContainerFactory")
    public void recibir(Message mensaje) {
        EventoEntrante evento = parsear(mensaje);
        if (evento.eventId() == null || evento.eventType() == null || evento.eventType().isBlank()) {
            throw new AmqpRejectAndDontRequeueException(
                    "El sobre del evento no trae eventId o eventType; se rechaza sin reencolar");
        }

        String correlacion = evento.correlationId();
        if (correlacion == null || correlacion.isBlank()) {
            Object cabecera = mensaje.getMessageProperties().getHeaders().get("X-Correlation-ID");
            correlacion = cabecera == null ? null : cabecera.toString();
        }

        String correlacionPrevia = MDC.get(CorrelationIdFilter.MDC_KEY);
        try {
            if (correlacion != null && !correlacion.isBlank()) {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlacion);
            }
            procesarEvento(evento);
        } finally {
            if (correlacionPrevia == null) {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            } else {
                MDC.put(CorrelationIdFilter.MDC_KEY, correlacionPrevia);
            }
        }
    }

    private void procesarEvento(EventoEntrante evento) {
        try {
            switch (evento.eventType()) {
                case "InstrumentoCreado" -> {
                    InstrumentoCreadoPayload payload = objectMapper.convertValue(evento.payload(), InstrumentoCreadoPayload.class);
                    catalogoEventService.procesarInstrumentoCreado(payload);
                }
                case "InstrumentoActualizado" -> {
                    InstrumentoActualizadoPayload payload = objectMapper.convertValue(evento.payload(), InstrumentoActualizadoPayload.class);
                    catalogoEventService.procesarInstrumentoActualizado(payload);
                }
                case "MercadoCreado" -> {
                    MercadoCreadoPayload payload = objectMapper.convertValue(evento.payload(), MercadoCreadoPayload.class);
                    catalogoEventService.procesarMercadoCreado(payload);
                }
                case "MercadoActualizado" -> {
                    MercadoActualizadoPayload payload = objectMapper.convertValue(evento.payload(), MercadoActualizadoPayload.class);
                    catalogoEventService.procesarMercadoActualizado(payload);
                }
                default -> log.debug("Evento no manejado: {}", evento.eventType());
            }
        } catch (Exception ex) {
            log.warn("Error procesando evento {}: {}", evento.eventType(), ex.getMessage());
            throw ex;
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
            log.warn("Mensaje inválido recibido en la cola {}; se rechaza sin reencolar", RoutingKeys.COLA_M4_EVENTOS, ex);
            throw new AmqpRejectAndDontRequeueException("JSON inválido en la cola " + RoutingKeys.COLA_M4_EVENTOS, ex);
        }
    }
}