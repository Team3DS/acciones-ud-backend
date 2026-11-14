package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m4datosmercado.application.service.CatalogoEventoService;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CatalogoEventoConsumer {

    private static final Logger log = LoggerFactory.getLogger(CatalogoEventoConsumer.class);

    private final CatalogoEventoService servicio;
    private final ObjectMapper objectMapper;

    public CatalogoEventoConsumer(CatalogoEventoService servicio, ObjectMapper objectMapper) {
        this.servicio = servicio;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_CATALOGO)
    public void procesarEvento(String mensaje, @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        try {
            EventoCatalogoEntrante evento = objectMapper.readValue(mensaje, EventoCatalogoEntrante.class);
            Map<String, Object> payload = evento.payload();
            String symbol = asString(payload.get("symbol"));
            String name = asString(payload.get("name"));
            String market = asString(payload.get("market"));
            String currency = asString(payload.get("currency"));

            switch (routingKey) {
                case "m3.instrumento.creado" ->
                        servicio.procesarInstrumentoCreado(symbol, name, market, currency);
                case "m3.instrumento.actualizado" ->
                        servicio.procesarInstrumentoActualizado(symbol, name, market, currency);
                case "m3.mercado.creado" ->
                        servicio.procesarMercadoCreado(symbol, name, market, currency);
                case "m3.mercado.actualizado" ->
                        servicio.procesarMercadoActualizado(symbol, name, market, currency);
                default -> log.warn("Routing key desconocida: {}", routingKey);
            }
            log.info("Evento procesado: {} -> {}", routingKey, symbol);
        } catch (Exception e) {
            log.error("Error procesando evento de catalogo: {}", e.getMessage(), e);
        }
    }

    private String asString(Object valor) {
        return valor != null ? String.valueOf(valor) : null;
    }
}
