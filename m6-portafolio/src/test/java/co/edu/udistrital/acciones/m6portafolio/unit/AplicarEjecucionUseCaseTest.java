package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionesActualizadasPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.TipoMovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AplicarEjecucionUseCaseTest {

    private static final long USUARIO = 4101L;
    private static final long OTRO = 9999L;
    private static final Instant T0 = Instant.parse("2026-01-02T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-02T12:00:00Z"), ZoneOffset.UTC);
    private static final String CLAVE = "EXE:E-2";

    @Mock
    private PosicionRepositoryPort posiciones;

    @Mock
    private ReservaPosicionRepositoryPort reservas;

    @Mock
    private MovimientoPosicionRepositoryPort movimientos;

    @Mock
    private OutboxPort outbox;

    private AplicarEjecucionUseCase caso;

    @BeforeEach
    void prepararCaso() {
        caso = new AplicarEjecucionUseCase(posiciones, reservas, movimientos,
                new ValoracionService(CLOCK), outbox, CLOCK);
    }

    @Test
    void aplicaLaPrimeraCompraConCostoPromedioYMovimiento() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL"))
                .thenReturn(Optional.of(posicionConId(USUARIO, "AAPL")));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EjecucionResponse respuesta = caso.ejecutar(
                peticion(USUARIO, "aapl", LadoEjecucion.BUY, "10", "180.00", T0),
                new Actor(USUARIO, false, false));

        assertThat(respuesta.applied()).isTrue();
        assertThat(respuesta.position().quantity()).isEqualByComparingTo("10");
        assertThat(respuesta.position().averageCost()).isEqualByComparingTo("180.0000");
        assertThat(respuesta.position().currentPrice()).isEqualByComparingTo("180.0000");
        verify(posiciones).crearSiNoExiste(eq(USUARIO), eq("AAPL"), any(Instant.class));
        verify(movimientos).save(any(MovimientoPosicion.class));
    }

    @Test
    void registraElMovimientoConLaClaveExeYLosDatosDeLaEjecucion() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "MSFT"))
                .thenReturn(Optional.of(posicionConId(USUARIO, "MSFT")));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        caso.ejecutar(peticion(USUARIO, "MSFT", LadoEjecucion.BUY, "3", "10.5000", T0),
                new Actor(USUARIO, false, false));

        verify(movimientos).save(argThat(movimiento ->
                movimiento.getTipo() == TipoMovimientoPosicion.COMPRA
                        && movimiento.getIdempotencyKey().equals(CLAVE)
                        && movimiento.getExecutionId().equals("E-2")
                        && movimiento.getOrderId().equals("O-2")
                        && movimiento.getCantidad().compareTo(new BigDecimal("3")) == 0
                        && movimiento.getPrecio().compareTo(new BigDecimal("10.5")) == 0));
    }

    @Test
    void emiteElEventoPosicionesActualizadasConElEstadoDeLaPosicion() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL"))
                .thenReturn(Optional.of(posicionConId(USUARIO, "AAPL")));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        caso.ejecutar(peticion(USUARIO, "AAPL", LadoEjecucion.BUY, "10", "180.00", T0),
                new Actor(USUARIO, false, false));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(eq(RoutingKeys.TIPO_AGREGADO_POSICION), eq("1"),
                eq(RoutingKeys.POSICIONES_ACTUALIZADAS), captor.capture());
        PosicionesActualizadasPayload evento = (PosicionesActualizadasPayload) captor.getValue();
        assertThat(evento.orderId()).isEqualTo("O-2");
        assertThat(evento.executionId()).isEqualTo("E-2");
        assertThat(evento.userId()).isEqualTo(USUARIO);
        assertThat(evento.symbol()).isEqualTo("AAPL");
        assertThat(evento.side()).isEqualTo("BUY");
        assertThat(evento.quantity()).isEqualByComparingTo("10");
        assertThat(evento.price()).isEqualByComparingTo("180.00");
        assertThat(evento.newQuantity()).isEqualByComparingTo("10");
        assertThat(evento.averageCost()).isEqualByComparingTo("180.0000");
        assertThat(evento.realizedPnl()).isEqualByComparingTo("0");
    }

    @Test
    void unaCompraRepetidaDevuelveAppliedFalseSinCambios() {
        Posicion existente = Posicion.abrir(USUARIO, "AAPL", T0);
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(true);
        when(posiciones.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.of(existente));

        EjecucionResponse respuesta = caso.ejecutar(
                peticion(USUARIO, "AAPL", LadoEjecucion.BUY, "10", "180.00", T0),
                new Actor(USUARIO, false, false));

        assertThat(respuesta.applied()).isFalse();
        assertThat(respuesta.position().quantity()).isEqualByComparingTo("0");
        verify(posiciones, never()).crearSiNoExiste(any(), any(), any());
        verify(posiciones, never()).save(any(Posicion.class));
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void repiteLaRespuestaSiLaClaveApareceDespuesDeBloquearLaPosicion() {
        Posicion existente = Posicion.abrir(USUARIO, "AAPL", T0);
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false, true);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL"))
                .thenReturn(Optional.of(existente));

        EjecucionResponse respuesta = caso.ejecutar(
                peticion(USUARIO, "AAPL", LadoEjecucion.BUY, "10", "180.00", T0),
                new Actor(USUARIO, false, false));

        assertThat(respuesta.applied()).isFalse();
        assertThat(existente.getCantidad()).isEqualByComparingTo("0");
        verify(movimientos, times(2)).existsByIdempotencyKey(CLAVE);
        verify(posiciones, never()).save(any(Posicion.class));
    }

    @Test
    void unaRepeticionSinPosicionRespondeConflicto() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(true);
        when(posiciones.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.empty());

        EjecucionRequest peticion = peticion(USUARIO, "AAPL", LadoEjecucion.BUY, "10", "180.00", T0);
        Actor actor = new Actor(USUARIO, false, false);

        assertThatThrownBy(() -> caso.ejecutar(peticion, actor))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void elActorSistemaOperaSobreCualquierUsuario() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(OTRO, "AAPL"))
                .thenReturn(Optional.of(posicionConId(OTRO, "AAPL")));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EjecucionResponse respuesta = caso.ejecutar(
                peticion(OTRO, "AAPL", LadoEjecucion.BUY, "1", "5.0000", T0), Actor.interno());

        assertThat(respuesta.applied()).isTrue();
        assertThat(respuesta.position().userId()).isEqualTo(OTRO);
    }

    @Test
    void unUsuarioAjenoNoPuedeOperar() {
        EjecucionRequest peticion = peticion(OTRO, "AAPL", LadoEjecucion.BUY, "1", "5.0000", T0);
        Actor actor = new Actor(USUARIO, false, false);

        assertThatThrownBy(() -> caso.ejecutar(peticion, actor))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(movimientos, never()).existsByIdempotencyKey(any());
    }

    @Test
    void unaVentaReservadaActualizaElPnlYEmiteElEvento() {
        Posicion posicion = posicionConId(USUARIO, "AAPL");
        posicion.comprar(new BigDecimal("10"), new BigDecimal("185.00"), T0);
        posicion.reservar(new BigDecimal("4"), T0.plusSeconds(1));
        ReservaPosicion reserva = ReservaPosicion.crear(1L, "O-2", new BigDecimal("4"),
                "RSV:clave-2", T0.plusSeconds(1));
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));
        when(reservas.findByOrderIdForUpdate("O-2")).thenReturn(Optional.of(reserva));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EjecucionResponse respuesta = caso.ejecutar(
                peticion(USUARIO, "AAPL", LadoEjecucion.SELL, "4", "200.00", T0.plusSeconds(2)),
                new Actor(USUARIO, false, false));

        assertThat(respuesta.applied()).isTrue();
        assertThat(respuesta.position().quantity()).isEqualByComparingTo("6");
        assertThat(respuesta.position().reservedQuantity()).isEqualByComparingTo("0");
        assertThat(respuesta.position().realizedPnl()).isEqualByComparingTo("60.0000");
        assertThat(reserva.getCantidadEjecutada()).isEqualByComparingTo("4");
        assertThat(reserva.getEstado().name()).isEqualTo("CONFIRMED");
        verify(reservas).save(reserva);
        verify(movimientos).save(argThat(movimiento ->
                movimiento.getTipo() == TipoMovimientoPosicion.VENTA
                        && movimiento.getIdempotencyKey().equals(CLAVE)));
    }

    @Test
    void unaVentaSinPosicionRespondeVentaNoDisponible() {
        when(movimientos.existsByIdempotencyKey(CLAVE)).thenReturn(false);
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caso.ejecutar(
                peticion(USUARIO, "AAPL", LadoEjecucion.SELL, "1", "200.00", T0),
                new Actor(USUARIO, false, false)))
                .isInstanceOf(VentaNoDisponibleException.class)
                .hasMessageContaining(VentaNoDisponibleException.CODIGO);
        verify(posiciones, never()).crearSiNoExiste(any(), any(), any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void usaElRelojCuandoLaEjecucionNoTraeInstante() {
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL"))
                .thenReturn(Optional.of(posicionConId(USUARIO, "AAPL")));
        when(posiciones.save(any(Posicion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EjecucionResponse respuesta = caso.ejecutar(
                new EjecucionRequest("O-5", "E-5", USUARIO, "AAPL", LadoEjecucion.BUY,
                        BigDecimal.TEN, new BigDecimal("180.00"), null),
                new Actor(USUARIO, false, false));

        assertThat(respuesta.position().priceTimestamp()).isEqualTo(Instant.now(CLOCK));
    }

    private static EjecucionRequest peticion(long userId, String symbol, LadoEjecucion lado,
                                             String cantidad, String precio, Instant ejecutadoEn) {
        return new EjecucionRequest("O-2", "E-2", userId, symbol, lado,
                new BigDecimal(cantidad), new BigDecimal(precio), ejecutadoEn);
    }

    private static Posicion posicionConId(long userId, String symbol) {
        Posicion posicion = Posicion.abrir(userId, symbol, T0);
        ReflectionTestUtils.setField(posicion, "id", 1L);
        return posicion;
    }
}
