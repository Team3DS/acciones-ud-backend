package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosLiberadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoReserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiberarReservaUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private ReservaRepositoryPort reservas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    @Mock
    private OutboxPort outbox;

    private LiberarReservaUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new LiberarReservaUseCase(cuentas, reservas,
                new LibroMovimientos(movimientos, outbox, CLOCK), outbox, CLOCK);
    }

    @Test
    void liberaLaReservaYDevuelveElMontoAlDisponible() {
        Cuenta cuenta = cuentaConReserva(10L, 5L, "1000.00", "300.00");
        Reserva reserva = reserva(77L, 10L, "300.00");
        prepararBloqueo(cuenta, reserva);
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 99L);
            return movimiento;
        });

        ReservaResponse respuesta = casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.status()).isEqualTo("RELEASED");
        assertThat(respuesta.available()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("0.00");
        assertThat(respuesta.total()).isEqualByComparingTo("1000.00");
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.RELEASED);
        assertThat(reserva.getMotivoLiberacion()).isEqualTo("OrdenCancelada");
        verify(cuentas).save(cuenta);
        verify(reservas).save(reserva);

        ArgumentCaptor<Movimiento> captorMovimiento = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captorMovimiento.capture());
        assertThat(captorMovimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.LIBERACION);
        assertThat(captorMovimiento.getValue().getMonto()).isEqualByComparingTo("300.00");
        assertThat(captorMovimiento.getValue().getIdempotencyKey()).isEqualTo("LIB:77");

        ArgumentCaptor<Object> captorEvento = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(eq("Reserva"), eq("77"), eq("m2.fondos.liberados"), captorEvento.capture());
        assertThat(captorEvento.getValue()).isEqualTo(
                new FondosLiberadosPayload(77L, "OrdenCancelada"));
    }

    @Test
    void repetirSobreUnaReservaLiberadaNoCambiaNada() {
        Cuenta cuenta = cuentaConReserva(10L, 5L, "1000.00", "300.00");
        cuenta.liberarReserva(new BigDecimal("300.00"), AHORA);
        Reserva reserva = reserva(77L, 10L, "300.00");
        reserva.liberar("OrdenCancelada", AHORA);
        prepararBloqueo(cuenta, reserva);

        ReservaResponse respuesta = casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false));

        assertThat(respuesta.status()).isEqualTo("RELEASED");
        assertThat(respuesta.available()).isEqualByComparingTo("1000.00");
        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
        verify(movimientos, never()).save(any());
        verify(outbox, never()).registrar(any(), any(), any(), any());
    }

    @Test
    void repetirSobreUnaReservaExpiradaNoCambiaNada() {
        Cuenta cuenta = cuentaConReserva(10L, 5L, "1000.00", "300.00");
        cuenta.liberarReserva(new BigDecimal("300.00"), AHORA);
        Reserva reserva = reserva(77L, 10L, "300.00");
        ReflectionTestUtils.setField(reserva, "estado", EstadoReserva.EXPIRED);
        prepararBloqueo(cuenta, reserva);

        ReservaResponse respuesta = casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("Expirada"), new Actor(5L, false));

        assertThat(respuesta.status()).isEqualTo("EXPIRED");
        assertThat(respuesta.available()).isEqualByComparingTo("1000.00");
        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void noLiberaUnaReservaConfirmada() {
        Cuenta cuenta = cuentaConReserva(10L, 5L, "1000.00", "300.00");
        Reserva reserva = reserva(77L, 10L, "300.00");
        reserva.confirmar("ejecucion-1", AHORA);
        prepararBloqueo(cuenta, reserva);

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false)))
                .isInstanceOf(TransicionReservaInvalidaException.class)
                .hasMessageContaining("CONFIRMED");

        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void responde404SiNoExisteLaReserva() {
        when(reservas.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("77");

        verifyNoInteractions(cuentas, movimientos, outbox);
    }

    @Test
    void responde404SiNoExisteLaCuentaDeLaReserva() {
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva(77L, 10L, "300.00")));
        when(cuentas.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(movimientos, outbox);
    }

    @Test
    void soloElDuenoPuedeLiberar() {
        when(reservas.findById(77L)).thenReturn(Optional.of(reserva(77L, 10L, "300.00")));
        when(cuentas.findByIdForUpdate(10L)).thenReturn(Optional.of(cuentaConReserva(10L, 5L, "1000.00", "300.00")));

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(1L, true)))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(reservas, never()).findByIdForUpdate(any());
        verifyNoInteractions(movimientos, outbox);
    }

    @Test
    void noLiberaSiLaCuentaEstaBloqueada() {
        Cuenta cuenta = cuentaConReserva(10L, 5L, "1000.00", "300.00");
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);
        Reserva reserva = reserva(77L, 10L, "300.00");
        prepararBloqueo(cuenta, reserva);

        assertThatThrownBy(() -> casoDeUso.ejecutar(77L,
                new LiberarReservaRequest("OrdenCancelada"), new Actor(5L, false)))
                .isInstanceOf(CuentaNoOperableException.class);

        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
    }

    private void prepararBloqueo(Cuenta cuenta, Reserva reserva) {
        when(reservas.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(cuentas.findByIdForUpdate(cuenta.getId())).thenReturn(Optional.of(cuenta));
        when(reservas.findByIdForUpdate(reserva.getId())).thenReturn(Optional.of(reserva));
    }

    private Cuenta cuentaConReserva(Long id, Long userId, String disponible, String reservado) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        cuenta.abonar(new BigDecimal(disponible), AHORA);
        cuenta.reservar(new BigDecimal(reservado), AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Reserva reserva(Long id, Long cuentaId, String monto) {
        Reserva reserva = Reserva.crear(cuentaId, "orden-" + id, new BigDecimal(monto), "RES:clave-" + id, AHORA);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }
}
