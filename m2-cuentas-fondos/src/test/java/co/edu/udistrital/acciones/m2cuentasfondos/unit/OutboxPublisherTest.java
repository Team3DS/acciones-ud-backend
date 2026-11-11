package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoCreadoPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.messaging.OutboxPublisher;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxPublisherTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    private final OutboxEventoRepositoryPort outbox = mock(OutboxEventoRepositoryPort.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OutboxPublisher publisher = new OutboxPublisher(outbox, objectMapper, CLOCK);

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void registraElEventoConCorrelacionYPayloadJson() throws Exception {
        MDC.put(CorrelationIdFilter.MDC_KEY, "trace-outbox");

        publisher.registrar("Movimiento", "55", RoutingKeys.MOVIMIENTO_CREADO,
                new MovimientoCreadoPayload(55L, 5L, "DEPOSITO",
                        new BigDecimal("20.00"), new BigDecimal("80.00")));

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(outbox).save(captor.capture());
        OutboxEvento evento = captor.getValue();
        assertThat(evento.getId()).isNotNull();
        assertThat(evento.getAggregateType()).isEqualTo("Movimiento");
        assertThat(evento.getAggregateId()).isEqualTo("55");
        assertThat(evento.getEventType()).isEqualTo(RoutingKeys.MOVIMIENTO_CREADO);
        assertThat(evento.getCorrelationId()).isEqualTo("trace-outbox");
        assertThat(evento.getCreadoEn()).isEqualTo(AHORA);
        assertThat(evento.isPublicado()).isFalse();
        assertThat(evento.getReintentos()).isZero();

        JsonNode payload = objectMapper.readTree(evento.getPayload());
        assertThat(payload.get("movementId").asLong()).isEqualTo(55L);
        assertThat(payload.get("userId").asLong()).isEqualTo(5L);
        assertThat(payload.get("type").asText()).isEqualTo("DEPOSITO");
        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(payload.get("balance").decimalValue()).isEqualByComparingTo("80.00");
    }

    @Test
    void dejaLaCorrelacionNulaSiNoHayMdc() {
        publisher.registrar("Movimiento", "55", RoutingKeys.MOVIMIENTO_CREADO, new MovimientoCreadoPayload(
                55L, 5L, "DEPOSITO", BigDecimal.ONE, BigDecimal.ONE));

        ArgumentCaptor<OutboxEvento> captor = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(outbox).save(captor.capture());
        assertThat(captor.getValue().getCorrelationId()).isNull();
    }
}
