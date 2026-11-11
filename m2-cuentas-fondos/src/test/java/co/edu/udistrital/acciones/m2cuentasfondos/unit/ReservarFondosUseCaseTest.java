package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosReservadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ReservaResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoReservaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ReservarFondosUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservarFondosUseCaseTest {

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

    private ReservarFondosUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new ReservarFondosUseCase(cuentas, reservas, new LibroMovimientos(movimientos, outbox, CLOCK),
                outbox, CLOCK);
    }

    @Test
    void reservaElMontoYMueveElDisponibleAlReservado() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(reservas.findByOrderId("orden-1")).thenReturn(Optional.empty());
        when(reservas.save(any(Reserva.class))).thenAnswer(invocacion -> {
            Reserva reserva = invocacion.getArgument(0);
            ReflectionTestUtils.setField(reserva, "id", 77L);
            return reserva;
        });
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 99L);
            return movimiento;
        });

        ReservaResponse respuesta = casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-1", new Actor(5L, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.orderId()).isEqualTo("orden-1");
        assertThat(respuesta.userId()).isEqualTo(5L);
        assertThat(respuesta.amount()).isEqualByComparingTo("300.00");
        assertThat(respuesta.status()).isEqualTo("PENDING");
        assertThat(respuesta.available()).isEqualByComparingTo("700.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("300.00");
        assertThat(respuesta.total()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.currency()).isEqualTo("USD");
        assertThat(respuesta.createdAt()).isEqualTo(AHORA);
        verify(cuentas).save(cuenta);

        ArgumentCaptor<Reserva> captorReserva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservas).save(captorReserva.capture());
        assertThat(captorReserva.getValue().getIdempotencyKey()).isEqualTo("RES:clave-1");
        assertThat(captorReserva.getValue().getMonto()).isEqualByComparingTo("300.00");

        ArgumentCaptor<Movimiento> captorMovimiento = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captorMovimiento.capture());
        assertThat(captorMovimiento.getValue().getTipo()).isEqualTo(TipoMovimiento.RESERVA);
        assertThat(captorMovimiento.getValue().getIdempotencyKey()).isEqualTo("RES:77");
        assertThat(captorMovimiento.getValue().getSaldoPosterior()).isEqualByComparingTo("700.00");
        assertThat(captorMovimiento.getValue().getDescripcion()).contains("orden-1");

        ArgumentCaptor<Object> captorEvento = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(eq("Movimiento"), eq("99"), eq("m2.movimiento.creado"), any());
        verify(outbox).registrar(eq("Reserva"), eq("77"), eq("m2.fondos.reservados"), captorEvento.capture());
        assertThat(captorEvento.getValue()).isEqualTo(
                new FondosReservadosPayload(77L, "orden-1", 5L, new BigDecimal("300.00")));
    }

    @Test
    void repiteLaRespuestaCuandoLaClaveYaExisteConLosMismosDatos() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);
        Reserva existente = reserva(77L, 10L, "orden-1", new BigDecimal("300.00"), "RES:clave-1");
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta));

        ReservaResponse respuesta = casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-1", new Actor(5L, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        assertThat(respuesta.status()).isEqualTo("PENDING");
        assertThat(respuesta.available()).isEqualByComparingTo("700.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("300.00");
        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void detectaLaRepeticionDespuesDeBloquearLaCuenta() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);
        Reserva existente = reserva(77L, 10L, "orden-1", new BigDecimal("300.00"), "RES:clave-1");
        when(reservas.findByIdempotencyKey("RES:clave-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existente));
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta));

        ReservaResponse respuesta = casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-1", new Actor(5L, false));

        assertThat(respuesta.reservationId()).isEqualTo(77L);
        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
    }

    @Test
    void rechazaLaMismaClaveConOtroMonto() {
        Cuenta cuenta = cuenta(10L, 5L);
        Reserva existente = reserva(77L, 10L, "orden-1", new BigDecimal("50.00"), "RES:clave-1");
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-1", new Actor(5L, false)))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void rechazaLaMismaClaveConOtraOrden() {
        Cuenta cuenta = cuenta(10L, 5L);
        Reserva existente = reserva(77L, 10L, "otra-orden", new BigDecimal("300.00"), "RES:clave-1");
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findById(10L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-1", new Actor(5L, false)))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void responde409SiLaOrdenYaTieneReserva() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        when(reservas.findByIdempotencyKey("RES:clave-2")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(reservas.findByOrderId("orden-1"))
                .thenReturn(Optional.of(reserva(77L, 10L, "orden-1", new BigDecimal("300.00"), "RES:clave-1")));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("300.00"), null), "clave-2", new Actor(5L, false)))
                .isInstanceOf(ConflictoReservaException.class)
                .hasMessageContaining("orden-1");

        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
    }

    @Test
    void responde422SiNoExisteLaCuenta() {
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("10.00"), null), "clave-1", new Actor(5L, false)))
                .isInstanceOf(FondosInsuficientesException.class)
                .hasMessageContaining("5");

        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
    }

    @Test
    void responde422SiNoAlcanzaElDisponible() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(reservas.findByOrderId("orden-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("100.01"), null), "clave-1", new Actor(5L, false)))
                .isInstanceOf(FondosInsuficientesException.class);

        verify(cuentas, never()).save(any());
        verify(reservas, never()).save(any());
    }

    @Test
    void noReservaSiLaCuentaEstaBloqueada() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);
        when(reservas.findByIdempotencyKey("RES:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(reservas.findByOrderId("orden-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("10.00"), null), "clave-1", new Actor(5L, false)))
                .isInstanceOf(CuentaNoOperableException.class);

        verify(cuentas, never()).save(any());
    }

    @Test
    void soloElDuenoPuedeReservar() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new ReservaRequest("orden-1", 5L, new BigDecimal("10.00"), null), "clave-1", new Actor(1L, true)))
                .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(cuentas, reservas, movimientos, outbox);
    }

    private Cuenta cuenta(Long id, Long userId) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Reserva reserva(Long id, Long cuentaId, String orden, BigDecimal monto, String clave) {
        Reserva reserva = Reserva.crear(cuentaId, orden, monto, clave, AHORA);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }
}
