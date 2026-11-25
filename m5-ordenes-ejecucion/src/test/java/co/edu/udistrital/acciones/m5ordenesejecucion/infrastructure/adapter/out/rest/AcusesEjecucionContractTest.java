package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.service.CancelarOrdenUseCaseImpl;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.messaging.OrdenMessageConsumer;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class AcusesEjecucionContractTest {
    private static final String BROKER_ORDER_ID = "alpaca-real-order";
    private static final String BROKER_EXECUTION_ID = "alpaca-real-order-3";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final OrdenRepositoryPort orders = mock(OrdenRepositoryPort.class);
    private final EjecucionOrdenRepositoryPort executions =
            mock(EjecucionOrdenRepositoryPort.class);
    private final ConfirmacionEjecucionRepositoryPort confirmations =
            mock(ConfirmacionEjecucionRepositoryPort.class);
    private final FondosClientPort funds = mock(FondosClientPort.class);
    private final PortafolioClientPort portfolio = mock(PortafolioClientPort.class);
    private final AlpacaOrderPort alpaca = mock(AlpacaOrderPort.class);
    private final PublicadorOrdenesPort events = mock(PublicadorOrdenesPort.class);
    private final Map<ConfirmationKey, ConfirmacionEjecucion> received = new HashMap<>();
    private final Orden order = new Orden();
    private OrdenMessageConsumer consumer;
    private CancelarOrdenUseCaseImpl cancel;

    @BeforeEach
    void prepare() {
        order.setId(10L);
        order.setUsuarioId(7L);
        order.setTicker("AAPL");
        order.setCantidad(5);
        order.setLadoOrden(LadoOrden.COMPRA);
        order.setEstadoOrden(EstadoOrden.PARCIALMENTE_EJECUTADA);
        order.setBrokerOrderId(BROKER_ORDER_ID);
        order.setReservaFondosId(70L);
        EjecucionOrden execution =
                new EjecucionOrden(
                        42L,
                        10L,
                        BROKER_EXECUTION_ID,
                        3,
                        new BigDecimal("100.00"),
                        new BigDecimal("0.60"),
                        Instant.now(),
                        Instant.now());
        when(orders.findById(10L)).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(executions.findByBrokerExecutionId(BROKER_EXECUTION_ID))
                .thenReturn(Optional.of(execution));
        when(executions.findByOrdenId(10L)).thenReturn(List.of(execution));
        when(confirmations.findByEjecucionIdAndServicio(anyLong(), any()))
                .thenAnswer(
                        invocation ->
                                Optional.ofNullable(
                                        received.get(
                                                new ConfirmationKey(
                                                        invocation.getArgument(0),
                                                        invocation.getArgument(1)))));
        when(confirmations.save(any()))
                .thenAnswer(
                        invocation -> {
                            ConfirmacionEjecucion confirmation = invocation.getArgument(0);
                            received.put(
                                    new ConfirmationKey(
                                            confirmation.getEjecucionOrdenId(),
                                            confirmation.getServicioParticipante()),
                                    confirmation);
                            return confirmation;
                        });
        when(alpaca.cancelarOrden(BROKER_ORDER_ID))
                .thenReturn(
                        new AlpacaOrderPort.AlpacaOrderResponse(
                                BROKER_ORDER_ID,
                                null,
                                "canceled",
                                "AAPL",
                                5,
                                new BigDecimal("100.00"),
                                3));
        var history = mock(HistorialEstadoOrdenRepositoryPort.class);
        var outbox = mock(OutboxEventoRepositoryPort.class);
        consumer =
                new OrdenMessageConsumer(
                        orders,
                        history,
                        confirmations,
                        funds,
                        portfolio,
                        alpaca,
                        events,
                        mapper,
                        executions);
        cancel =
                new CancelarOrdenUseCaseImpl(
                        orders,
                        history,
                        outbox,
                        funds,
                        portfolio,
                        alpaca,
                        events,
                        mapper,
                        executions,
                        confirmations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"M2", "M6"})
    void acusesDeSobreRealResuelvenElFillYDesbloqueanCancelacionSoloTrasAmbos(String first)
            throws Exception {
        assertCancellationBlocked();
        Message fundsConfirmed =
                message(
                        RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS,
                        Map.of(
                                "reservationId",
                                70,
                                "executionId",
                                BROKER_EXECUTION_ID,
                                "amount",
                                new BigDecimal("300.60")));
        Message portfolioUpdated =
                message(
                        RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS,
                        Map.of(
                                "orderId",
                                "10",
                                "executionId",
                                BROKER_EXECUTION_ID,
                                "userId",
                                7,
                                "symbol",
                                "AAPL",
                                "side",
                                "BUY",
                                "quantity",
                                3,
                                "price",
                                new BigDecimal("100.00"),
                                "newQuantity",
                                3,
                                "averageCost",
                                new BigDecimal("98.25"),
                                "realizedPnl",
                                BigDecimal.ZERO));
        Message firstMessage = first.equals("M2") ? fundsConfirmed : portfolioUpdated;
        Message secondMessage = first.equals("M2") ? portfolioUpdated : fundsConfirmed;

        consumer.recibirMensaje(firstMessage);
        consumer.recibirMensaje(firstMessage);
        assertThat(received).hasSize(1);
        assertCancellationBlocked();

        consumer.recibirMensaje(secondMessage);
        consumer.recibirMensaje(secondMessage);

        assertThat(received)
                .containsOnlyKeys(
                        new ConfirmationKey(42L, ServicioParticipante.FONDOS),
                        new ConfirmationKey(42L, ServicioParticipante.PORTAFOLIO));
        assertThat(
                        received.get(new ConfirmationKey(42L, ServicioParticipante.FONDOS))
                                .getPrecioPromedioCosto())
                .isNull();
        assertThat(
                        received.get(new ConfirmationKey(42L, ServicioParticipante.PORTAFOLIO))
                                .getPrecioPromedioCosto())
                .isEqualByComparingTo("98.25");
        verify(confirmations, times(2)).save(any());

        cancel.ejecutar(10L, 7L, "Cancelar remanente");
        cancel.ejecutar(10L, 7L, "Cancelar remanente");

        assertThat(order.getEstadoOrden()).isEqualTo(EstadoOrden.CANCELADA);
        verify(funds, times(1)).liberarReserva(70L, 7L, "Cancelar remanente");
        verify(events, times(1))
                .publicarOrdenCancelada(any(), eq(10L), eq(7L), eq("Cancelar remanente"));
        verify(portfolio, never()).liberarReserva(anyLong(), anyLong(), anyString());
    }

    private void assertCancellationBlocked() {
        assertThatThrownBy(() -> cancel.ejecutar(10L, 7L, "Cancelar remanente"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getEstadoOrden()).isEqualTo(EstadoOrden.PARCIALMENTE_EJECUTADA);
        verify(funds, never()).liberarReserva(anyLong(), anyLong(), anyString());
        verify(events, never()).publicarOrdenCancelada(any(), any(), any(), any());
    }

    private Message message(String routingKey, Map<String, Object> payload) throws Exception {
        UUID eventId = UUID.randomUUID();
        byte[] body =
                mapper.writeValueAsBytes(
                        Map.of(
                                "eventId",
                                eventId.toString(),
                                "eventType",
                                routingKey,
                                "occurredAt",
                                Instant.now().toString(),
                                "aggregateId",
                                "70",
                                "correlationId",
                                "fixture-correlation",
                                "payload",
                                payload));
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(routingKey);
        properties.setMessageId(eventId.toString());
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        return new Message(body, properties);
    }

    private record ConfirmationKey(Long executionId, ServicioParticipante service) {}
}
