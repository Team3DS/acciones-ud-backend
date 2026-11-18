package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoEstadoReservaException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.TipoMovimientoPosicion;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiberarReservaUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-02-03T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final Long USUARIO = 4101L;
    private static final Long RESERVA_ID = 77L;
    private static final Long POSICION_ID = 5L;

    @Mock
    private PosicionRepositoryPort posiciones;

    @Mock
    private ReservaPosicionRepositoryPort reservas;

    @Mock
    private MovimientoPosicionRepositoryPort movimientos;

    private LiberarReservaUseCase caso;

    @BeforeEach
    void prepararCaso() {
        caso = new LiberarReservaUseCase(posiciones, reservas, movimientos, CLOCK);
    }

    @Test
    void liberarDevuelveLasAccionesYRegistraElMovimiento() {
        Posicion posicion = posicionReservada("10");
        ReservaPosicion reserva = reservaPendiente();
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));
        when(reservas.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        ReservaPosicionResponse respuesta = caso.ejecutar(RESERVA_ID, "cancelada",
                new Actor(USUARIO, false, false));

        assertThat(respuesta.reservationId()).isEqualTo(RESERVA_ID);
        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.RELEASED.name());
        assertThat(respuesta.reason()).isEqualTo("cancelada");
        assertThat(respuesta.releasedAt()).isEqualTo(AHORA);
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("0");
        assertThat(posicion.disponible()).isEqualByComparingTo("20");
        verify(posiciones).save(posicion);
        verify(reservas).save(reserva);

        ArgumentCaptor<MovimientoPosicion> captor = ArgumentCaptor.forClass(MovimientoPosicion.class);
        verify(movimientos).save(captor.capture());
        MovimientoPosicion movimiento = captor.getValue();
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoPosicion.LIBERACION);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("10");
        assertThat(movimiento.getPrecio()).isEqualByComparingTo("185.00");
        assertThat(movimiento.getExecutionId()).isNull();
        assertThat(movimiento.getOrderId()).isEqualTo("ORD-1");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("LIB:77");
    }

    @Test
    void unaReservaLiberadaRespondeSinCambios() {
        Posicion posicion = posicionReservada("0");
        ReservaPosicion reserva = reservaPendiente();
        reserva.liberar("previa", AHORA.minusSeconds(10));
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));
        when(reservas.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        ReservaPosicionResponse respuesta = caso.ejecutar(RESERVA_ID, "otra",
                new Actor(USUARIO, false, false));

        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.RELEASED.name());
        assertThat(respuesta.reason()).isEqualTo("previa");
        verify(posiciones, never()).save(any(Posicion.class));
        verify(reservas, never()).save(any(ReservaPosicion.class));
        verify(movimientos, never()).save(any(MovimientoPosicion.class));
    }

    @Test
    void liberarDespuesDeUnaEjecucionParcialDevuelveSoloLoPendiente() {
        Posicion posicion = posicionReservada("10");
        ReservaPosicion reserva = reservaPendiente();
        reserva.ejecutar(new BigDecimal("4"), AHORA.minusSeconds(10));
        posicion.venderReservado(new BigDecimal("4"), new BigDecimal("200.00"), AHORA.minusSeconds(10));
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));
        when(reservas.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        ReservaPosicionResponse respuesta = caso.ejecutar(RESERVA_ID, "cancelada",
                new Actor(USUARIO, false, false));

        assertThat(respuesta.executedQuantity()).isEqualByComparingTo("4");
        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.RELEASED.name());
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("0");
        ArgumentCaptor<MovimientoPosicion> captor = ArgumentCaptor.forClass(MovimientoPosicion.class);
        verify(movimientos).save(captor.capture());
        assertThat(captor.getValue().getCantidad()).isEqualByComparingTo("6");
    }

    @Test
    void unaReservaConfirmadaResponde409() {
        Posicion posicion = posicionReservada("10");
        ReservaPosicion reserva = mock(ReservaPosicion.class);
        when(reserva.getPosicionId()).thenReturn(POSICION_ID);
        when(reserva.getEstado()).thenReturn(EstadoReservaPosicion.CONFIRMED);
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));
        when(reservas.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> caso.ejecutar(RESERVA_ID, "cancelada",
                new Actor(USUARIO, false, false)))
                .isInstanceOf(ConflictoEstadoReservaException.class)
                .hasMessageContaining("confirmada");
        verify(movimientos, never()).save(any(MovimientoPosicion.class));
    }

    @Test
    void responde404CuandoNoExisteLaReserva() {
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caso.ejecutar(RESERVA_ID, "cancelada",
                new Actor(USUARIO, false, false)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verifyNoInteractions(posiciones, movimientos);
    }

    @Test
    void unUsuarioAjenoResponde403() {
        Posicion posicion = posicionReservada("10");
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reservaPendiente()));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));

        assertThatThrownBy(() -> caso.ejecutar(RESERVA_ID, "cancelada",
                new Actor(4999L, false, false)))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(reservas, never()).findByIdForUpdate(any());
        verify(movimientos, never()).save(any(MovimientoPosicion.class));
    }

    @Test
    void unTokenDeSistemaPuedeLiberarCualquierReserva() {
        Posicion posicion = posicionReservada("10");
        ReservaPosicion reserva = reservaPendiente();
        when(reservas.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(posiciones.findByIdForUpdate(POSICION_ID)).thenReturn(Optional.of(posicion));
        when(reservas.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));

        ReservaPosicionResponse respuesta = caso.ejecutar(RESERVA_ID, "cancelada", Actor.interno());

        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.RELEASED.name());
        assertThat(respuesta.userId()).isEqualTo(USUARIO);
    }

    private static Posicion posicionReservada(String cantidadReservada) {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", AHORA.minusSeconds(120));
        posicion.comprar(new BigDecimal("20"), new BigDecimal("185.00"), AHORA.minusSeconds(120));
        if (new BigDecimal(cantidadReservada).signum() > 0) {
            posicion.reservar(new BigDecimal(cantidadReservada), AHORA.minusSeconds(60));
        }
        ReflectionTestUtils.setField(posicion, "id", POSICION_ID);
        return posicion;
    }

    private static ReservaPosicion reservaPendiente() {
        ReservaPosicion reserva = ReservaPosicion.crear(POSICION_ID, "ORD-1", new BigDecimal("10"),
                "RSV:clave-1", AHORA.minusSeconds(60));
        ReflectionTestUtils.setField(reserva, "id", RESERVA_ID);
        return reserva;
    }
}
