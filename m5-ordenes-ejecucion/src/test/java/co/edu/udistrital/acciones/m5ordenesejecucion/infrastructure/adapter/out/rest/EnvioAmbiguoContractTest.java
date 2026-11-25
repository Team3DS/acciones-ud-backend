package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.CrearOrdenCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.service.CrearOrdenUseCaseImpl;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.service.ProcesarEjecucionUseCaseImpl;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.service.SincronizarEjecucionesService;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.messaging.OrdenMessageConsumer;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class EnvioAmbiguoContractTest {
    private static final UUID CLIENT_ID = UUID.fromString("9240ea78-d1dd-44ab-836f-055517dc4d73");
    private static final String BROKER_ID = "broker-order-confirmed";
    private final OrdenRepositoryPort orders = mock(OrdenRepositoryPort.class);
    private final EjecucionOrdenRepositoryPort executions =
            mock(EjecucionOrdenRepositoryPort.class);
    private final FondosClientPort funds = mock(FondosClientPort.class);
    private final PortafolioClientPort portfolio = mock(PortafolioClientPort.class);
    private final DatosMercadoClientPort market = mock(DatosMercadoClientPort.class);
    private final PublicadorOrdenesPort events = mock(PublicadorOrdenesPort.class);
    private final HistorialEstadoOrdenRepositoryPort history =
            mock(HistorialEstadoOrdenRepositoryPort.class);
    private final ConfirmacionEjecucionRepositoryPort confirmations =
            mock(ConfirmacionEjecucionRepositoryPort.class);
    private final AtomicReference<Orden> stored = new AtomicReference<>();
    private final List<EjecucionOrden> fills = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private MockRestServiceServer broker;
    private AlpacaOrderAdapter alpaca;
    private CrearOrdenUseCaseImpl create;
    private SincronizarEjecucionesService synchronize;

    @BeforeEach
    void prepare() {
        RestTemplate http = new RestTemplate();
        broker = MockRestServiceServer.bindTo(http).build();
        alpaca = new AlpacaOrderAdapter(http, "http://alpaca", "fixture-key", "fixture-secret");
        var commissions = mock(ConfiguracionComisionRepositoryPort.class);
        var outbox = mock(OutboxEventoRepositoryPort.class);
        ConfiguracionComision commission = new ConfiguracionComision();
        commission.setId(3L);
        commission.setPorcentaje(new BigDecimal("0.002"));
        when(commissions.findVigentePorMercado(eq("NYSE"), any()))
                .thenReturn(Optional.of(commission));
        when(commissions.findById(3L)).thenReturn(Optional.of(commission));
        when(orders.save(any()))
                .thenAnswer(
                        invocation -> {
                            Orden order = invocation.getArgument(0);
                            if (order.getId() == null) order.setId(10L);
                            stored.set(order);
                            return order;
                        });
        when(orders.findByClientOrderId(CLIENT_ID))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(orders.findById(10L)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(orders.findPendientesDeEjecucion()).thenAnswer(invocation -> List.of(stored.get()));
        when(executions.findByOrdenId(10L)).thenAnswer(invocation -> List.copyOf(fills));
        when(executions.findByBrokerExecutionId(anyString()))
                .thenAnswer(
                        invocation ->
                                fills.stream()
                                        .filter(
                                                fill ->
                                                        fill.getBrokerExecutionId()
                                                                .equals(invocation.getArgument(0)))
                                        .findFirst());
        when(executions.save(any()))
                .thenAnswer(
                        invocation -> {
                            EjecucionOrden fill = invocation.getArgument(0);
                            fill.setId(100L + fills.size());
                            fills.add(fill);
                            return fill;
                        });
        when(funds.reservarFondos(eq("10"), eq(42L), any(), eq("RES:" + CLIENT_ID))).thenReturn(7L);
        when(portfolio.actualizarPosicion(
                        anyString(),
                        anyString(),
                        eq(42L),
                        eq("AAPL"),
                        eq(LadoOrden.COMPRA),
                        eq(5),
                        any(),
                        any(),
                        anyString()))
                .thenReturn(new BigDecimal("101.25"));
        create =
                new CrearOrdenUseCaseImpl(
                        orders,
                        commissions,
                        history,
                        outbox,
                        funds,
                        portfolio,
                        market,
                        alpaca,
                        events,
                        mapper);
        var process =
                new ProcesarEjecucionUseCaseImpl(
                        orders,
                        commissions,
                        executions,
                        confirmations,
                        history,
                        outbox,
                        funds,
                        portfolio,
                        events,
                        mapper);
        synchronize =
                new SincronizarEjecucionesService(orders, executions, alpaca, market, process);
    }

    @ParameterizedTest
    @ValueSource(strings = {"timeout", "502"})
    void envioSinConfirmacionConservaReservaYRecuperaElFillSinReenviar(String failure) {
        expectAmbiguousSendAndRecovery(failure);
        when(market.esMercadoAbierto("NYSE")).thenReturn(true);

        Orden order = create.ejecutar(command());

        assertPendingSubmission(order);
        assertThat(create.ejecutar(command())).isSameAs(order);
        verify(funds, times(1)).reservarFondos(eq("10"), eq(42L), any(), eq("RES:" + CLIENT_ID));
        recoverAndCheckFill(order);
    }

    @Test
    void aperturaDeMercadoMantieneEnSeguimientoLaOrdenEncoladaTrasTimeout() {
        expectAmbiguousSendAndRecovery("timeout");
        when(market.esMercadoAbierto("NYSE")).thenReturn(false);
        Orden order = create.ejecutar(command());
        order.setConfirmadoEncolamientoEn(Instant.now());
        when(orders.findEncoladasListasParaEnvio())
                .thenAnswer(
                        invocation ->
                                order.getEstadoOrden() == EstadoOrden.PENDIENTE
                                        ? List.of(order)
                                        : List.of());
        var consumer =
                new OrdenMessageConsumer(
                        orders, history, confirmations, funds, portfolio, alpaca, events, mapper, executions);
        MessageProperties properties = new MessageProperties();
        properties.setReceivedRoutingKey(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO);
        Message opening = new Message("{}".getBytes(StandardCharsets.UTF_8), properties);

        consumer.recibirMensaje(opening);
        assertPendingSubmission(order);
        consumer.recibirMensaje(opening);
        verify(funds, times(1)).reservarFondos(eq("10"), eq(42L), any(), eq("RES:" + CLIENT_ID));
        recoverAndCheckFill(order);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void falloAlPublicarCreadaNoRechazaUnaOrdenYaEnviadaAlBroker(boolean pending) {
        broker.expect(requestTo("http://alpaca/v2/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.client_order_id").value(CLIENT_ID.toString()))
                .andRespond(
                        pending
                                ? withException(
                                        new SocketTimeoutException(
                                                "Request sent; response missing"))
                                : withSuccess(
                                        "{\"id\":\"" + BROKER_ID + "\",\"status\":\"accepted\"}",
                                        MediaType.APPLICATION_JSON));
        when(market.esMercadoAbierto("NYSE")).thenReturn(true);
        doThrow(new IllegalStateException("Publisher unavailable"))
                .when(events)
                .publicarOrdenCreada(any(), any(), any(), any(), any(), any(), any());

        Orden order = create.ejecutar(command());

        assertThat(order.getEstadoOrden()).isEqualTo(EstadoOrden.ENVIADA);
        assertThat(order.getBrokerOrderId())
                .isEqualTo(pending ? AlpacaOrderPort.PREFIJO_PENDIENTE + CLIENT_ID : BROKER_ID);
        assertThat(order.getReservaFondosId()).isEqualTo(7L);
        assertThat(fills).isEmpty();
        verify(events, never()).publicarOrdenRechazada(any(), any(), any(), any(), any());
        verify(funds, never()).liberarReserva(anyLong(), anyLong(), anyString());
        broker.verify();
    }

    private void expectAmbiguousSendAndRecovery(String failure) {
        broker.expect(requestTo("http://alpaca/v2/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.client_order_id").value(CLIENT_ID.toString()))
                .andRespond(
                        "timeout".equals(failure)
                                ? withException(
                                        new SocketTimeoutException(
                                                "No response after sending request"))
                                : withStatus(HttpStatus.BAD_GATEWAY));
        String lookup = "http://alpaca/v2/orders:by_client_order_id?client_order_id=" + CLIENT_ID;
        broker.expect(requestTo(lookup))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        broker.expect(requestTo(lookup))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                "{\"id\":\""
                                        + BROKER_ID
                                        + "\",\"client_order_id\":\""
                                        + CLIENT_ID
                                        + "\",\"status\":\"filled\",\"symbol\":\"AAPL\",\"qty\":\"5\","
                                        + "\"filled_qty\":\"5\",\"filled_avg_price\":\"101.25\"}",
                                MediaType.APPLICATION_JSON));
    }

    private void assertPendingSubmission(Orden order) {
        assertThat(order.getEstadoOrden()).isEqualTo(EstadoOrden.ENVIADA);
        assertThat(order.getBrokerOrderId())
                .isEqualTo(AlpacaOrderPort.PREFIJO_PENDIENTE + CLIENT_ID);
        assertThat(order.getReservaFondosId()).isEqualTo(7L);
        assertThat(fills).isEmpty();
        verify(events, never())
                .publicarOrdenRechazada(any(), anyLong(), anyLong(), anyString(), anyString());
        verify(funds, never()).liberarReserva(anyLong(), anyLong(), anyString());
    }

    private void recoverAndCheckFill(Orden order) {
        assertThat(synchronize.sincronizar()).isZero();
        assertPendingSubmission(order);
        assertThat(synchronize.sincronizar()).isEqualTo(1);
        assertThat(order.getBrokerOrderId()).isEqualTo(BROKER_ID);
        assertThat(order.getEstadoOrden()).isEqualTo(EstadoOrden.EJECUTADA);
        assertThat(fills)
                .singleElement()
                .satisfies(
                        fill -> {
                            assertThat(fill.getBrokerExecutionId()).isEqualTo(BROKER_ID + "-5");
                            assertThat(fill.getCantidad()).isEqualTo(5);
                            assertThat(fill.getPrecioEjecucion()).isEqualByComparingTo("101.25");
                        });
        assertThat(synchronize.sincronizar()).isZero();
        verify(funds)
                .confirmarReserva(eq(7L), eq(42L), eq(BROKER_ID + "-5"), any(), eq(true), eq(1));
        verify(funds, never()).liberarReserva(anyLong(), anyLong(), anyString());
        verify(market, never()).obtenerPrecioEstimado(anyString());
        verify(events, never())
                .publicarOrdenRechazada(any(), anyLong(), anyLong(), anyString(), anyString());
        broker.verify();
    }

    private CrearOrdenCommand command() {
        return new CrearOrdenCommand(
                42L,
                1L,
                "AAPL",
                TipoOrden.MARKET,
                LadoOrden.COMPRA,
                VigenciaOrden.DIA,
                5,
                null,
                null,
                new BigDecimal("101.25"),
                CLIENT_ID);
    }
}
