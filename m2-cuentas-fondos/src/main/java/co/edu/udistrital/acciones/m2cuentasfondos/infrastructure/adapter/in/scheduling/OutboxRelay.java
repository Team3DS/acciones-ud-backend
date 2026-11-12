package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.EventoDominio;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxRelay {

    public static final String CABECERA_CORRELACION = "X-Correlation-ID";

    private final OutboxEventoRepositoryPort outbox;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int lote;

    public OutboxRelay(OutboxEventoRepositoryPort outbox, RabbitTemplate rabbitTemplate,
                       ObjectMapper objectMapper, Clock clock,
                       @Value("${app.outbox.lote:50}") int lote) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        List<OutboxEvento> pendientes = outbox.buscarPendientes(lote);
        for (OutboxEvento evento : pendientes) {
            publicar(evento);
        }
    }

    private void publicar(OutboxEvento evento) {
        try {
            EventoDominio sobre = construirSobre(evento);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, evento.getEventType(), sobre, mensaje -> {
                MessageProperties propiedades = mensaje.getMessageProperties();
                propiedades.setMessageId(evento.getId().toString());
                propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
                if (evento.getCorrelationId() != null) {
                    propiedades.setHeader(CABECERA_CORRELACION, evento.getCorrelationId());
                }
                return mensaje;
            });
            evento.marcarPublicado(Instant.now(clock));
        } catch (AmqpException | JsonProcessingException ex) {
            evento.registrarFallo(mensajeError(ex));
        }
        outbox.save(evento);
    }

    private EventoDominio construirSobre(OutboxEvento evento) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(evento.getPayload());
        return new EventoDominio(evento.getId(), evento.getEventType(), evento.getCreadoEn(),
                evento.getAggregateId(), evento.getCorrelationId(), payload);
    }

    private String mensajeError(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
    }
}
