package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.in.scheduling;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EventoSaliente;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
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

    private final OutboxEventoRepositoryPort almacen;
    private final RabbitTemplate broker;
    private final ObjectMapper lectorJson;
    private final Clock reloj;
    private final int tamanoLote;

    public OutboxRelay(OutboxEventoRepositoryPort repositorio, RabbitTemplate rabbitTemplate,
                       ObjectMapper json, Clock clock, @Value("${app.outbox.lote:50}") int lote) {
        this.almacen = repositorio;
        this.broker = rabbitTemplate;
        this.lectorJson = json;
        this.reloj = clock;
        this.tamanoLote = lote;
    }

    @Scheduled(fixedDelayString = "${app.outbox.intervalo-ms:1000}")
    @Transactional
    public void publicarPendientes() {
        List<OutboxEvento> pendientes = almacen.buscarPendientes(tamanoLote);
        pendientes.forEach(this::despachar);
    }

    private void despachar(OutboxEvento evento) {
        try {
            EventoSaliente sobre = armarSobre(evento);
            broker.convertAndSend(RoutingKeys.EXCHANGE, evento.getEventType(), sobre,
                    prepararCabeceras(evento));
            evento.confirmarPublicacion(Instant.now(reloj));
        } catch (JsonProcessingException | RuntimeException ex) {
            evento.registrarReintento(mensajeError(ex));
        }
        almacen.guardar(evento);
    }

    private MessagePostProcessor prepararCabeceras(OutboxEvento evento) {
        return mensaje -> {
            MessageProperties propiedades = mensaje.getMessageProperties();
            propiedades.setMessageId(evento.getId().toString());
            propiedades.setContentType(MediaType.APPLICATION_JSON_VALUE);
            propiedades.getHeaders().remove(CLAVE_TIPO_MENSAJE);
            if (evento.getCorrelationId() != null) {
                propiedades.setHeader(CABECERA_CORRELACION, evento.getCorrelationId());
            }
            return mensaje;
        };
    }

    private EventoSaliente armarSobre(OutboxEvento evento) throws JsonProcessingException {
        return new EventoSaliente(evento.getId(), evento.getEventType(), evento.getCreadoEn(),
                RoutingKeys.ORIGEN, evento.getAggregateId(), evento.getCorrelationId(),
                lectorJson.readTree(evento.getPayload()));
    }

    private static String mensajeError(Exception ex) {
        String mensaje = ex.getMessage();
        return mensaje == null || mensaje.isBlank() ? ex.getClass().getName() : mensaje;
    }
}
