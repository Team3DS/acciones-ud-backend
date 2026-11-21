package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m6portafolio.application.dto.EventoSaliente;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.OutboxEvento;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
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

    public static final String CABECERA_CORRELACION = CorrelationIdFilter.HEADER;
    private static final String CLAVE_TIPO_MENSAJE = "__TypeId__";
    private static final String ORIGEN = "m6";

    private final OutboxEventoRepositoryPort repositorio;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper json;
    private final Clock clock;
    private final int lote;

    public OutboxRelay(OutboxEventoRepositoryPort repositorio, RabbitTemplate rabbitTemplate,
                       ObjectMapper json, Clock clock, @Value("${app.outbox.lote:50}") int lote) {
        this.repositorio = repositorio;
        this.rabbitTemplate = rabbitTemplate;
        this.json = json;
        this.clock = clock;
        this.lote = lote;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        List<OutboxEvento> pendientes = repositorio.buscarPendientes(lote);
        for (OutboxEvento evento : pendientes) {
            despachar(evento);
        }
    }

    private void despachar(OutboxEvento evento) {
        Instant ahora = Instant.now(clock);
        try {
            EventoSaliente sobre = construirSobre(evento);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, evento.getEventType(), sobre,
                    mensaje -> completarCabeceras(mensaje, evento));
            evento.publicar(ahora);
        } catch (JsonProcessingException | RuntimeException ex) {
            evento.anotarFallo(describir(ex));
        }
        repositorio.guardar(evento);
    }

    private Message completarCabeceras(Message mensaje, OutboxEvento evento) {
        MessageProperties propiedades = mensaje.getMessageProperties();
        propiedades.setMessageId(evento.getId().toString());
        propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
        propiedades.getHeaders().remove(CLAVE_TIPO_MENSAJE);
        if (evento.getCorrelationId() != null) {
            propiedades.setHeader(CABECERA_CORRELACION, evento.getCorrelationId());
        }
        return mensaje;
    }

    private EventoSaliente construirSobre(OutboxEvento evento) throws JsonProcessingException {
        return new EventoSaliente(evento.getId(), evento.getEventType(), evento.getCreadoEn(), ORIGEN,
                evento.getAggregateId(), evento.getCorrelationId(), json.readTree(evento.getPayload()));
    }

    private static String describir(Exception ex) {
        String mensaje = ex.getMessage();
        return mensaje == null || mensaje.isBlank() ? ex.getClass().getName() : mensaje;
    }
}
