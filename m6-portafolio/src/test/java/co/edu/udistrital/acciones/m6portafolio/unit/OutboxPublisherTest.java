package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionesActualizadasPayload;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.OutboxEvento;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.messaging.OutboxPublisher;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
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
        PosicionesActualizadasPayload payload = new PosicionesActualizadasPayload("O-1", "E-1", 4101L,
                "AAPL", "BUY", new BigDecimal("10"), new BigDecimal("180.00"),
                new BigDecimal("20"), new BigDecimal("185.0000"), BigDecimal.ZERO);

        publicador.registrar("posicion", "77", "m6.posiciones.actualizadas", payload);

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(repositorio).guardar(captor.capture());
        OutboxEvento evento = captor.getValue();
        assertThat(evento.getId()).isNotNull();
        assertThat(evento.getAggregateType()).isEqualTo("posicion");
        assertThat(evento.getAggregateId()).isEqualTo("77");
        assertThat(evento.getEventType()).isEqualTo("m6.posiciones.actualizadas");
        assertThat(evento.getCorrelationId()).isEqualTo("corr-2026");
        assertThat(evento.getCreadoEn()).isEqualTo(AHORA);
        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isZero();
        assertThat(evento.getUltimoError()).isNull();
        assertThat(evento.getPayload())
                .contains("\"executionId\":\"E-1\"")
                .contains("\"symbol\":\"AAPL\"")
                .contains("\"newQuantity\":20");
    }

    @Test
    void guardaElEventoSinCorrelationCuandoElMdcEstaVacio() {
        OutboxPublisher publicador = new OutboxPublisher(repositorio, json, CLOCK);

        publicador.registrar("posicion", "78", "m6.posiciones.actualizadas", java.util.Map.of("k", "v"));

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(repositorio).guardar(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isNull();
    }

    @Test
    void fallaCuandoElPayloadNoSePuedeSerializar() {
        OutboxPublisher publicador = new OutboxPublisher(repositorio, json, CLOCK);
        Autorreferente payload = new Autorreferente();

        assertThatThrownBy(() -> publicador.registrar("posicion", "79", "m6.posiciones.actualizadas", payload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("m6.posiciones.actualizadas");
    }

    static final class Autorreferente {

        public Autorreferente getYo() {
            return this;
        }
    }
}
