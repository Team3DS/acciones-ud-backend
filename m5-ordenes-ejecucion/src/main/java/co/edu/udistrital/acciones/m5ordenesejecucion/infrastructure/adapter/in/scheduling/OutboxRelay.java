package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventoRepositoryPort outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final int lote;

    public OutboxRelay(OutboxEventoRepositoryPort outboxRepository,
                       RabbitTemplate rabbitTemplate,
                       @Value("${app.outbox.lote:50}") int lote) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void procesarOutbox() {
        List<OutboxEvento> pendientes = outboxRepository.findPendientes(lote);
        if (pendientes.isEmpty()) {
            return;
        }

        for (OutboxEvento evento : pendientes) {
            String routingKey = resolverRoutingKey(evento.getTipoEvento());
            try {
                rabbitTemplate.send(RabbitConfig.EXCHANGE, routingKey,
                        org.springframework.amqp.core.MessageBuilder.withBody(evento.getCarga().getBytes())
                                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                                .setMessageId(evento.getId().toString())
                                .build());

                evento.marcarPublicado();
                outboxRepository.save(evento);
            } catch (Exception ex) {
                log.warn("Fallo al publicar evento outbox {}: {}", evento.getId(), ex.getMessage());
                evento.registrarFallo(ex.getMessage());
                outboxRepository.save(evento);
            }
        }
    }

    private String resolverRoutingKey(String tipoEvento) {
        return switch (tipoEvento) {
            case "OrdenCreada" -> "m5.orden.creada";
            case "OrdenEjecutada" -> "m5.orden.ejecutada";
            case "OrdenCancelada" -> "m5.orden.cancelada";
            case "OrdenRechazada" -> "m5.orden.rechazada";
            default -> "m5.orden." + tipoEvento.toLowerCase();
        };
    }
}
