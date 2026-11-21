package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m6portafolio.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.service.ProcesarEventoEntranteService;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
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
public class PortafolioMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(PortafolioMessageConsumer.class);

    private final ObjectMapper json;
    private final ProcesarEventoEntranteService procesarEvento;

    public PortafolioMessageConsumer(ObjectMapper json, ProcesarEventoEntranteService procesarEvento) {
        this.json = json;
        this.procesarEvento = procesarEvento;
    }

    @RabbitListener(queues = RoutingKeys.COLA_POSICIONES, containerFactory = "portafolioListenerContainerFactory")
    public void recibir(Message mensaje) {
        EventoEntrante evento = parsear(mensaje);
        if (vacio(evento.eventId()) || vacio(evento.eventType())) {
            throw new AmqpRejectAndDontRequeueException(
                    "El sobre del evento no trae eventId o eventType; se rechaza sin reencolar");
        }
        String correlacion = correlacion(evento, mensaje);
        String previa = MDC.get(CorrelationIdFilter.CLAVE_MDC);
        try {
            if (!vacio(correlacion)) {
                MDC.put(CorrelationIdFilter.CLAVE_MDC, correlacion);
            }
            procesarEvento.procesar(evento);
        } finally {
            if (previa == null) {
                MDC.remove(CorrelationIdFilter.CLAVE_MDC);
            } else {
                MDC.put(CorrelationIdFilter.CLAVE_MDC, previa);
            }
        }
    }

    private EventoEntrante parsear(Message mensaje) {
        try {
            EventoEntrante evento = json.readValue(mensaje.getBody(), EventoEntrante.class);
            if (evento == null) {
                throw new AmqpRejectAndDontRequeueException("El cuerpo del mensaje esta vacio");
            }
            return evento;
        } catch (IOException ex) {
            log.warn("Mensaje invalido recibido en la cola {}; se rechaza sin reencolar",
                    RoutingKeys.COLA_POSICIONES, ex);
            throw new AmqpRejectAndDontRequeueException(
                    "JSON invalido en la cola " + RoutingKeys.COLA_POSICIONES, ex);
        }
    }

    private static String correlacion(EventoEntrante evento, Message mensaje) {
        if (!vacio(evento.correlationId())) {
            return evento.correlationId();
        }
        Object cabecera = mensaje.getMessageProperties().getHeaders().get(CorrelationIdFilter.HEADER);
        return cabecera == null ? null : cabecera.toString();
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
