package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.messaging.OutboxPublisher;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    private static final Instant AHORA = Instant.parse("2026-03-04T10:15:30Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private OutboxEventoRepositoryPort repositorio;

    private final ObjectMapper json = Jackson2ObjectMapperBuilder.json().build();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void guardaElEventoConElCorrelationDelMdcYElPayloadSerializado() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "corr-2026");
        OutboxPublisher publicador = new OutboxPublisher(repositorio, json, CLOCK);
        SuscripcionActivadaPayload payload = new SuscripcionActivadaPayload(
                4101L, "PREMIUM", "MENSUAL", "sub_abc", Instant.parse("2026-04-01T00:00:00Z"));

        publicador.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "7",
                RoutingKeys.SUSCRIPCION_ACTIVADA, payload);

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(repositorio).guardar(captor.capture());
        OutboxEvento evento = captor.getValue();
        assertThat(evento.getId()).isNotNull();
        assertThat(evento.getAggregateType()).isEqualTo("suscripcion");
        assertThat(evento.getAggregateId()).isEqualTo("7");
        assertThat(evento.getEventType()).isEqualTo("m10.suscripcion.activada");
        assertThat(evento.getCorrelationId()).isEqualTo("corr-2026");
        assertThat(evento.getCreadoEn()).isEqualTo(AHORA);
        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isZero();
        assertThat(evento.getUltimoError()).isNull();
        assertThat(evento.getPayload())
                .contains("\"userId\":4101")
                .contains("\"planId\":\"PREMIUM\"")
                .contains("\"interval\":\"MENSUAL\"")
                .contains("\"stripeSubscriptionId\":\"sub_abc\"");
    }

    @Test
    void guardaElEventoSinCorrelationCuandoElMdcEstaVacio() {
        OutboxPublisher publicador = new OutboxPublisher(repositorio, json, CLOCK);

        publicador.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "8",
                RoutingKeys.SUSCRIPCION_ACTIVADA, java.util.Map.of("k", "v"));

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(repositorio).guardar(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isNull();
    }

    @Test
    void fallaCuandoElPayloadNoSePuedeSerializar() {
        OutboxPublisher publicador = new OutboxPublisher(repositorio, json, CLOCK);
        Autorreferente payload = new Autorreferente();

        assertThatThrownBy(() -> publicador.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, "9",
                RoutingKeys.SUSCRIPCION_ACTIVADA, payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("m10.suscripcion.activada");
    }

    static final class Autorreferente {

        public Autorreferente getYo() {
            return this;
        }
    }
}
