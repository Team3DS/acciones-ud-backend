package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaPosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ReservarAccionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.TipoMovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.ReservaNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservarAccionesUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-02-03T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final Long USUARIO = 4101L;
    private static final String CLAVE = "00000000-0000-0000-0000-000000000001";
    private static final String CLAVE_UUID = UUID.nameUUIDFromBytes(("RSV:" + CLAVE).getBytes(StandardCharsets.UTF_8)).toString();

    @Mock
    private PosicionRepositoryPort posiciones;

    @Mock
    private ReservaPosicionRepositoryPort reservas;

    @Mock
    private MovimientoPosicionRepositoryPort movimientos;

    private ReservarAccionesUseCase caso;

    @BeforeEach
    void prepararCaso() {
        caso = new ReservarAccionesUseCase(posiciones, reservas, movimientos, CLOCK);
    }

    @Test
    void reservaElDisponibleYCreaLaReservaYElMovimiento() {
        Posicion posicion = posicion(7L, "20");
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.empty());
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));
        when(reservas.findByOrderId("1")).thenReturn(Optional.empty());
        when(reservas.save(any(ReservaPosicion.class))).thenAnswer(invocacion -> {
            ReservaPosicion reserva = invocacion.getArgument(0);
            ReflectionTestUtils.setField(reserva, "id", 77L);
            return reserva;
        });

        ReservaPosicionResponse respuesta = caso.ejecutar(
                peticion("1", "aapl", "10"), CLAVE, new Actor(USUARIO, false, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.orderId()).isEqualTo(1L);
        assertThat(respuesta.userId()).isEqualTo(USUARIO);
        assertThat(respuesta.symbol()).isEqualTo("AAPL");
        assertThat(respuesta.quantity()).isEqualTo(10);
        assertThat(respuesta.executedQuantity()).isEqualTo(0);
        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.ACTIVA.name());
        assertThat(respuesta.createdAt()).isEqualTo(AHORA);
        assertThat(posicion.getCantidadReservada()).isEqualByComparingTo("10");
        assertThat(posicion.disponible()).isEqualByComparingTo("10");
        verify(posiciones).save(posicion);

        ArgumentCaptor<MovimientoPosicion> captor = ArgumentCaptor.forClass(MovimientoPosicion.class);
        verify(movimientos).save(captor.capture());
        MovimientoPosicion movimiento = captor.getValue();
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimientoPosicion.RESERVA);
        assertThat(movimiento.getCantidad()).isEqualByComparingTo("10");
        assertThat(movimiento.getPrecio()).isEqualByComparingTo("185.00");
        assertThat(movimiento.getExecutionId()).isNull();
        assertThat(movimiento.getOrderId()).isEqualTo("1");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("RES:77");
        assertThat(movimiento.getOcurridoEn()).isEqualTo(AHORA);
    }

    @Test
    void unaClaveRepetidaConLosMismosDatosDevuelveLaReservaExistente() {
        Posicion posicion = posicion(7L, "20");
        ReservaPosicion existente = ReservaPosicion.crear(7L, 1L, 10,
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), AHORA.minusSeconds(60));
        ReflectionTestUtils.setField(existente, "id", 77L);
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.of(existente));
        when(posiciones.findById(7L)).thenReturn(Optional.of(posicion));

        ReservaPosicionResponse respuesta = caso.ejecutar(
                peticion("1", "AAPL", "10"), CLAVE, new Actor(USUARIO, false, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.status()).isEqualTo(EstadoReservaPosicion.ACTIVA.name());
        verify(reservas, never()).save(any(ReservaPosicion.class));
        verify(movimientos, never()).save(any(MovimientoPosicion.class));
        verify(posiciones, never()).findByUserIdAndSymbolForUpdate(any(), anyString());
    }

    @Test
    void unaClaveRepetidaConOtrosDatosResponde409() {
        Posicion posicion = posicion(7L, "20");
        ReservaPosicion existente = ReservaPosicion.crear(7L, 1L, 10,
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), AHORA.minusSeconds(60));
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.of(existente));
        when(posiciones.findById(7L)).thenReturn(Optional.of(posicion));

        assertThatThrownBy(() -> caso.ejecutar(peticion("1", "AAPL", "6"), CLAVE,
                new Actor(USUARIO, false, false)))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void unaOrdenYaReservadaConOtraClaveResponde409() {
        Posicion posicion = posicion(7L, "20");
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.empty());
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));
        when(reservas.findByOrderId("1"))
                .thenReturn(Optional.of(ReservaPosicion.crear(7L, 1L, 1,
                        UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff"), AHORA)));

        assertThatThrownBy(() -> caso.ejecutar(peticion("1", "AAPL", "10"), CLAVE,
                new Actor(USUARIO, false, false)))
                .isInstanceOf(ConflictoIdempotenciaException.class)
                .hasMessageContaining("1");
        verify(reservas, never()).save(any(ReservaPosicion.class));
    }

    @Test
    void responde422CuandoNoExisteLaPosicion() {
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.empty());
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caso.ejecutar(peticion("1", "AAPL", "10"), CLAVE,
                new Actor(USUARIO, false, false)))
                .isInstanceOf(ReservaNoDisponibleException.class)
                .hasMessageContaining("AAPL");
        verify(reservas, never()).save(any(ReservaPosicion.class));
    }

    @Test
    void responde422CuandoElDisponibleNoAlcanza() {
        Posicion posicion = posicion(7L, "5");
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.empty());
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));
        when(reservas.findByOrderId("1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caso.ejecutar(peticion("1", "AAPL", "10"), CLAVE,
                new Actor(USUARIO, false, false)))
                .isInstanceOf(ReservaNoDisponibleException.class);
        verify(posiciones, never()).save(any(Posicion.class));
        verify(movimientos, never()).save(any(MovimientoPosicion.class));
    }

    @Test
    void unUsuarioAjenoResponde403() {
        assertThatThrownBy(() -> caso.ejecutar(peticion("1", "AAPL", "10"), CLAVE,
                new Actor(4999L, false, false)))
                .isInstanceOf(AccesoDenegadoException.class);
        verifyNoInteractions(reservas, posiciones, movimientos);
    }

    @Test
    void unTokenDeSistemaPuedeReservarParaCualquierUsuario() {
        Posicion posicion = posicion(7L, "20");
        when(reservas.findByIdempotencyKey(CLAVE_UUID)).thenReturn(Optional.empty());
        when(posiciones.findByUserIdAndSymbolForUpdate(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));
        when(reservas.findByOrderId("1")).thenReturn(Optional.empty());
        when(reservas.save(any(ReservaPosicion.class))).thenAnswer(invocacion -> {
            ReservaPosicion reserva = invocacion.getArgument(0);
            ReflectionTestUtils.setField(reserva, "id", 78L);
            return reserva;
        });

        ReservaPosicionResponse respuesta = caso.ejecutar(peticion("1", "AAPL", "10"), CLAVE,
                Actor.interno());

        assertThat(respuesta.reservationId()).isEqualTo(78L);
        assertThat(respuesta.userId()).isEqualTo(USUARIO);
    }

    private static ReservaRequest peticion(String orderId, String symbol, String cantidad) {
        return new ReservaRequest(orderId, USUARIO, symbol, new BigDecimal(cantidad), CLAVE);
    }

    private static Posicion posicion(Long id, String cantidad) {
        Posicion posicion = Posicion.abrir(USUARIO, "AAPL", AHORA.minusSeconds(60));
        posicion.comprar(new BigDecimal(cantidad), new BigDecimal("185.00"), AHORA.minusSeconds(60));
        ReflectionTestUtils.setField(posicion, "id", id);
        return posicion;
    }
}