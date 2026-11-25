package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase.ProcesarEjecucionCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcesarEjecucionEventIdTest {

    @Test
    void reintentoTrasRollbackConservaEventIdDirectoYJsonOutboxPeroOtraEjecucionNo() throws Exception {
        OrdenRepositoryPort orders = mock(OrdenRepositoryPort.class);
        EjecucionOrdenRepositoryPort executions = mock(EjecucionOrdenRepositoryPort.class);
        OutboxEventoRepositoryPort outbox = mock(OutboxEventoRepositoryPort.class);
        PublicadorOrdenesPort publisher = mock(PublicadorOrdenesPort.class);
        PortafolioClientPort portfolio = mock(PortafolioClientPort.class);
        ObjectMapper mapper = new ObjectMapper();
        var service = new ProcesarEjecucionUseCaseImpl(orders, mock(ConfiguracionComisionRepositoryPort.class), executions,
                mock(ConfirmacionEjecucionRepositoryPort.class), mock(HistorialEstadoOrdenRepositoryPort.class), outbox,
                mock(FondosClientPort.class), portfolio, publisher, mapper);

        Orden order = new Orden();
        order.setId(4L);
        order.setUsuarioId(42L);
        order.setTicker("AAPL");
        order.setLadoOrden(LadoOrden.VENTA);
        order.setCantidad(1);
        when(orders.findById(4L)).thenReturn(Optional.of(order));
        // Cada intento vuelve a ver la ejecución ausente, como ocurre tras revertir la transacción local.
        when(executions.findByBrokerExecutionId(anyString())).thenReturn(Optional.empty());
        AtomicLong generatedIds = new AtomicLong();
        AtomicReference<EjecucionOrden> latest = new AtomicReference<>();
        when(executions.save(any())).thenAnswer(invocation -> {
            EjecucionOrden execution = invocation.getArgument(0);
            execution.setId(generatedIds.incrementAndGet());
            latest.set(execution);
            return execution;
        });
        when(executions.findByOrdenId(4L)).thenAnswer(invocation -> List.of(latest.get()));
        when(portfolio.actualizarPosicion(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new BigDecimal("99.0000"));

        Instant executedAt = Instant.parse("2026-11-25T18:00:00Z");
        for (String brokerExecutionId : List.of("broker-ejecución-1", "broker-ejecución-1", "broker-ejecución-2")) {
            order.setEstadoOrden(EstadoOrden.ENVIADA);
            service.ejecutar(new ProcesarEjecucionCommand(4L, brokerExecutionId, "broker-order-4", 1,
                    new BigDecimal("100.00"), new BigDecimal("0.20"), executedAt));
        }

        ArgumentCaptor<UUID> directIds = ArgumentCaptor.forClass(UUID.class);
        verify(publisher, times(3)).publicarOrdenEjecutada(directIds.capture(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any());
        List<UUID> ids = directIds.getAllValues();
        assertThat(ids.get(0)).isEqualTo(ids.get(1)).isNotEqualTo(ids.get(2));
        assertThat(ids.get(0)).isEqualTo(UUID.nameUUIDFromBytes(
                "M5:m5.orden.ejecutada:4:broker-ejecución-1".getBytes(StandardCharsets.UTF_8)));

        ArgumentCaptor<OutboxEvento> stored = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(outbox, times(3)).save(stored.capture());
        for (int attempt = 0; attempt < 3; attempt++) {
            var envelope = mapper.readTree(stored.getAllValues().get(attempt).getCarga());
            assertThat(envelope.path("eventId").asText()).isEqualTo(ids.get(attempt).toString());
            assertThat(envelope.path("eventType").asText()).isEqualTo(EventosOrden.ORDEN_EJECUTADA);
        }
    }

    @Test
    void mismoBrokerExecutionIdEnOtraOrdenPerteneceAOtroNamespace() {
        assertThat(EventosOrden.idEjecucion(4L, "fill-1"))
                .isNotEqualTo(EventosOrden.idEjecucion(5L, "fill-1"));
    }
}
