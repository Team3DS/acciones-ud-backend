package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.service.ProcesarEventoEntranteService;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.in.messaging.PortafolioMessageConsumer;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PortafolioMessageConsumerTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final ProcesarEventoEntranteService servicio = mock(ProcesarEventoEntranteService.class);
    private final PortafolioMessageConsumer consumidor = new PortafolioMessageConsumer(json, servicio);

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void parseaElSobreYPropagaLaCorrelacionAlMdc() {
        doAnswer(invocacion -> {
            assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isEqualTo("trace-sobre");
            return null;
        }).when(servicio).procesar(any(EventoEntrante.class));

        consumidor.recibir(mensaje("""
                {"eventId":"evento-1","eventType":"m5.orden.cancelada","occurredAt":"2026-11-11T10:00:00Z",
                 "aggregateId":"orden-1","correlationId":"trace-sobre","payload":{"orderId":"orden-1"}}
                """));

        ArgumentCaptor<EventoEntrante> captor = ArgumentCaptor.forClass(EventoEntrante.class);
        verify(servicio).procesar(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("evento-1");
        assertThat(captor.getValue().eventType()).isEqualTo(RoutingKeys.M5_ORDEN_CANCELADA);
        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }

    @Test
    void usaLaCabeceraDeCorrelacionSiElSobreNoLaTrae() {
        doAnswer(invocacion -> {
            assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isEqualTo("trace-cabecera");
            return null;
        }).when(servicio).procesar(any(EventoEntrante.class));

        MessageProperties propiedades = new MessageProperties();
        propiedades.setHeader(CorrelationIdFilter.HEADER, "trace-cabecera");
        Message mensaje = new Message("""
                {"eventId":"evento-2","eventType":"m5.orden.rechazada",
                 "payload":{"orderId":"orden-2"}}
                """.getBytes(StandardCharsets.UTF_8), propiedades);

        consumidor.recibir(mensaje);

        verify(servicio).procesar(any(EventoEntrante.class));
        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }

    @Test
    void aceptaElSobreV11ConSource() {
        consumidor.recibir(mensaje("""
                {"eventId":"evento-3","eventType":"m5.orden.ejecutada","occurredAt":"2026-11-11T10:00:00Z",
                 "source":"m5","payload":{"orderId":"orden-3","executionId":"ejecucion-3","fills":[]}}
                """));

        verify(servicio).procesar(any(EventoEntrante.class));
    }

    @Test
    void rechazaSinReencolarUnJsonInvalido() {
        assertThatThrownBy(() -> consumidor.recibir(mensaje("esto no es json")))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaUnSobreSinEventId() {
        assertThatThrownBy(() -> consumidor.recibir(mensaje("""
                {"eventType":"m5.orden.cancelada","payload":{"orderId":"orden-4"}}
                """)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaUnSobreSinEventType() {
        assertThatThrownBy(() -> consumidor.recibir(mensaje("""
                {"eventId":"evento-5","payload":{"orderId":"orden-5"}}
                """)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verifyNoInteractions(servicio);
    }

    private static Message mensaje(String cuerpo) {
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), new MessageProperties());
    }
}
