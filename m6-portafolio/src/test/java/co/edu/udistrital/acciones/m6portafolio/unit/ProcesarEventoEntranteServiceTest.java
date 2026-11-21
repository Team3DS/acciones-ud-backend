package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.EventoInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.InboxEventoPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ProcesarEventoEntranteService;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarEventoEntranteServiceTest {

    private static final Instant T0 = Instant.parse("2026-06-01T14:00:00Z");
    private static final Clock CLOCK = Clock.fixed(T0.plusSeconds(5), ZoneOffset.UTC);

    @Mock
    private InboxEventoPort inboxEventos;

    @Mock
    private ReservaPosicionRepositoryPort reservas;

    @Mock
    private PosicionRepositoryPort posiciones;

    @Mock
    private AplicarEjecucionUseCase aplicarEjecucion;

    @Mock
    private LiberarReservaUseCase liberarReserva;

    @Mock
    private PlatformTransactionManager gestor;

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    private ProcesarEventoEntranteService servicio;

    @BeforeEach
    void prepararServicio() {
        org.mockito.Mockito.reset(inboxEventos, reservas, posiciones, aplicarEjecucion, liberarReserva, gestor);
        lenient().when(gestor.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(inboxEventos.registrarSiNuevo(anyString(), anyString(), anyString(), anyString(),
                any(JsonNode.class), any())).thenReturn(true);
        servicio = new ProcesarEventoEntranteService(gestor, inboxEventos, reservas, posiciones,
                aplicarEjecucion, liberarReserva, json, CLOCK);
    }

    @Test
    void unaCompraConDatosDePosicionAgregaLosFillsYAplicaLaEjecucion() {
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-1", Map.of(
                "orderId", "1",
                "executionId", "1",
                "fills", List.of(fill("100.00", "10"), fill("130.00", "5")),
                "userId", 77L,
                "symbol", "aapl",
                "side", "BUY"));

        servicio.procesar(evento);

        ArgumentCaptor<EjecucionRequest> captor = ArgumentCaptor.forClass(EjecucionRequest.class);
        verify(aplicarEjecucion).ejecutar(captor.capture(), eq(Actor.interno()));
        EjecucionRequest peticion = captor.getValue();
        assertThat(peticion.orderId()).isEqualTo("1");
        assertThat(peticion.executionId()).isEqualTo("1");
        assertThat(peticion.userId()).isEqualTo(77L);
        assertThat(peticion.symbol()).isEqualTo("AAPL");
        assertThat(peticion.side()).isEqualTo(LadoEjecucion.BUY);
        assertThat(peticion.quantity()).isEqualByComparingTo("15");
        assertThat(peticion.price()).isEqualByComparingTo("110.0000");
        assertThat(peticion.executedAt()).isEqualTo(T0);
        assertThat(peticion.eventId()).isEqualTo("evento-1");
        verifyNoInteractions(liberarReserva);
    }

    @Test
    void unaVentaSinDatosOpcionalesUsaLaReservaPendiente() {
        ReservaPosicion reserva = reserva(9L, 5L, 2L);
        Posicion posicion = Posicion.abrir(88L, "MSFT", T0);
        when(reservas.findByOrderId("2")).thenReturn(Optional.of(reserva));
        when(posiciones.findById(5L)).thenReturn(Optional.of(posicion));
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-2", Map.of(
                "orderId", "2",
                "executionId", "2",
                "fills", List.of(fill("200.00", "4")),
                "totalAmount", new BigDecimal("800.00")));

        servicio.procesar(evento);

        ArgumentCaptor<EjecucionRequest> captor = ArgumentCaptor.forClass(EjecucionRequest.class);
        verify(aplicarEjecucion).ejecutar(captor.capture(), eq(Actor.interno()));
        assertThat(captor.getValue().userId()).isEqualTo(88L);
        assertThat(captor.getValue().symbol()).isEqualTo("MSFT");
        assertThat(captor.getValue().side()).isEqualTo(LadoEjecucion.SELL);
        assertThat(captor.getValue().quantity()).isEqualByComparingTo("4");
        assertThat(captor.getValue().price()).isEqualByComparingTo("200.0000");
        assertThat(captor.getValue().eventId()).isEqualTo("evento-2");
    }

    @Test
    void unaEjecucionSinDatosNiReservaSeDescarta() {
        when(reservas.findByOrderId("3")).thenReturn(Optional.empty());
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-3", Map.of(
                "orderId", "3",
                "executionId", "3",
                "fills", List.of(fill("100.00", "1"))));

        servicio.procesar(evento);

        verifyNoInteractions(aplicarEjecucion, liberarReserva);
    }

    @Test
    void unaEjecucionConReservaConfirmadaSeDescarta() {
        ReservaPosicion reserva = reserva(9L, 5L, 4L);
        reserva.consumir(10);
        when(reservas.findByOrderId("4")).thenReturn(Optional.of(reserva));
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-4", Map.of(
                "orderId", "4",
                "executionId", "4",
                "fills", List.of(fill("100.00", "1"))));

        servicio.procesar(evento);

        verifyNoInteractions(aplicarEjecucion, liberarReserva);
    }

    @Test
    void unaOrdenCanceladaLiberaLaReservaConElMotivo() {
        ReservaPosicion reserva = reserva(11L, 5L, 5L);
        when(reservas.findByOrderId("5")).thenReturn(Optional.of(reserva));
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_CANCELADA, "evento-5", Map.of(
                "orderId", "5",
                "reason", "Cancelada por el usuario"));

        servicio.procesar(evento);

        verify(liberarReserva).ejecutar(11L, "Cancelada por el usuario", Actor.interno());
        verifyNoInteractions(aplicarEjecucion);
    }

    @Test
    void unaOrdenRechazadaSinMotivoUsaElMotivoPorDefecto() {
        ReservaPosicion reserva = reserva(12L, 5L, 6L);
        when(reservas.findByOrderId("6")).thenReturn(Optional.of(reserva));
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_RECHAZADA, "evento-6", Map.of(
                "orderId", "6",
                "brokerError", "sin liquidez"));

        servicio.procesar(evento);

        verify(liberarReserva).ejecutar(12L, "OrdenRechazada", Actor.interno());
    }

    @Test
    void unaCancelacionSinReservaSeDescarta() {
        when(reservas.findByOrderId("7")).thenReturn(Optional.empty());
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_CANCELADA, "evento-7", Map.of(
                "orderId", "7"));

        servicio.procesar(evento);

        verifyNoInteractions(aplicarEjecucion, liberarReserva);
    }

    @Test
    void unEventoDesconocidoSeDescarta() {
        EventoEntrante evento = evento("m5.evento.desconocido", "evento-9", Map.of("orderId", "9"));

        servicio.procesar(evento);

        verifyNoInteractions(aplicarEjecucion, liberarReserva);
    }

    @Test
    void unFalloDeNegocioRegistraElEventoYNoLoPropaga() {
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-10", Map.of(
                "orderId", "10",
                "executionId", "10",
                "fills", List.of(fill("100.00", "1")),
                "userId", 77L,
                "symbol", "AAPL",
                "side", "BUY"));
        doThrow(new VentaNoDisponibleException("sin acciones"))
                .when(aplicarEjecucion).ejecutar(any(EjecucionRequest.class), any(Actor.class));

        servicio.procesar(evento);

        verify(inboxEventos, times(2)).registrarSiNuevo(anyString(), anyString(), anyString(), anyString(),
                any(JsonNode.class), any());
    }

    @Test
    void unFalloTecnicoSePropagaParaQueSeReintente() {
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-11", Map.of(
                "orderId", "11",
                "executionId", "11",
                "fills", List.of(fill("100.00", "1")),
                "userId", 77L,
                "symbol", "AAPL",
                "side", "BUY"));
        doThrow(new IllegalStateException("base de datos caida"))
                .when(aplicarEjecucion).ejecutar(any(EjecucionRequest.class), any(Actor.class));

        assertThatThrownBy(() -> servicio.procesar(evento))
                .isInstanceOf(IllegalStateException.class);

        verify(inboxEventos, times(1)).registrarSiNuevo(anyString(), anyString(), anyString(), anyString(),
                any(JsonNode.class), any());
    }

    @Test
    void unaEjecucionSinFillsEsInvalida() {
        EventoEntrante evento = evento(RoutingKeys.M5_ORDEN_EJECUTADA, "evento-12", Map.of(
                "orderId", "12",
                "executionId", "12",
                "fills", List.of(),
                "userId", 77L,
                "symbol", "AAPL",
                "side", "BUY"));

        assertThatThrownBy(() -> servicio.procesar(evento))
                .isInstanceOf(EventoInvalidoException.class);

        verify(aplicarEjecucion, never()).ejecutar(any(), any());
    }

    private EventoEntrante evento(String tipo, String eventId, Map<String, Object> payload) {
        return new EventoEntrante(eventId, tipo, T0, "agregado-1", "correlacion-1", json.valueToTree(payload));
    }

    private static Map<String, Object> fill(String precio, String cantidad) {
        return Map.of("price", new BigDecimal(precio), "qty", new BigDecimal(cantidad),
                "timestamp", T0.toString());
    }

    private static ReservaPosicion reserva(Long id, Long posicionId, Long orderId) {
        ReservaPosicion reserva = ReservaPosicion.crear(posicionId, orderId, 10,
                java.util.UUID.randomUUID(), T0);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }
}