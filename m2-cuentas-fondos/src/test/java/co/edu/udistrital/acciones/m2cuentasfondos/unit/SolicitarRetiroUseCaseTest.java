package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RetiroResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.SolicitarRetiroUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitarRetiroUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    @Mock
    private OutboxPort outbox;

    private SolicitarRetiroUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new SolicitarRetiroUseCase(cuentas, movimientos,
                new LibroMovimientos(movimientos, outbox, CLOCK), CLOCK);
    }

    @Test
    void registraElRetiroYGuardaElMovimiento() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 99L);
            return movimiento;
        });

        RetiroResponse respuesta = casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("200.00"), "Retiro parcial"),
                "clave-1", new Actor(5L, false));

        assertThat(respuesta.movementId()).isEqualTo(99L);
        assertThat(respuesta.userId()).isEqualTo(5L);
        assertThat(respuesta.amount()).isEqualByComparingTo("200.00");
        assertThat(respuesta.available()).isEqualByComparingTo("800.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("0.00");
        assertThat(respuesta.total()).isEqualByComparingTo("800.00");
        assertThat(respuesta.currency()).isEqualTo("USD");
        verify(cuentas).save(cuenta);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.RETIRO);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("RET:clave-1");
        assertThat(captor.getValue().getSaldoPosterior()).isEqualByComparingTo("800.00");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Retiro parcial");
        verify(outbox).registrar(org.mockito.ArgumentMatchers.eq("Movimiento"),
                org.mockito.ArgumentMatchers.eq("99"),
                org.mockito.ArgumentMatchers.eq("m2.movimiento.creado"), any());
    }

    @Test
    void repiteLaRespuestaCuandoLaClaveYaExisteConLosMismosDatos() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.cargar(new BigDecimal("200.00"), AHORA);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("200.00"), "RET:clave-1");
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        RetiroResponse respuesta = casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("200.00"), null),
                "clave-1", new Actor(5L, false));

        assertThat(respuesta.movementId()).isEqualTo(77L);
        assertThat(respuesta.available()).isEqualByComparingTo("800.00");
        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void detectaLaRepeticionDespuesDeBloquearLaCuenta() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.cargar(new BigDecimal("200.00"), AHORA);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("200.00"), "RET:clave-1");
        when(movimientos.findByIdempotencyKey("RET:clave-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existente));
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        RetiroResponse respuesta = casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("200.00"), null),
                "clave-1", new Actor(5L, false));

        assertThat(respuesta.movementId()).isEqualTo(77L);
        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void rechazaLaMismaClaveConOtroMonto() {
        Cuenta cuenta = cuenta(10L, 5L);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("50.00"), "RET:clave-1");
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("200.00"), null),
                "clave-1", new Actor(5L, false)))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void responde422SiNoExisteLaCuenta() {
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("10.00"), null),
                "clave-1", new Actor(5L, false)))
                .isInstanceOf(FondosInsuficientesException.class)
                .hasMessageContaining("5");

        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void responde422SiNoAlcanzaElDisponible() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("100.01"), null),
                "clave-1", new Actor(5L, false)))
                .isInstanceOf(FondosInsuficientesException.class);

        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void noRetiraSiLaCuentaEstaBloqueada() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);
        when(movimientos.findByIdempotencyKey("RET:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("10.00"), null),
                "clave-1", new Actor(5L, false)))
                .isInstanceOf(CuentaNoOperableException.class);

        verify(cuentas, never()).save(any());
    }

    @Test
    void soloElDuenoPuedeRetirar() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new RetiroRequest(5L, new BigDecimal("10.00"), null),
                "clave-1", new Actor(1L, true)))
                .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(cuentas, movimientos);
    }

    private Cuenta cuenta(Long id, Long userId) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Movimiento movimiento(Long id, Long cuentaId, BigDecimal monto, String clave) {
        Movimiento movimiento = Movimiento.crear(cuentaId, TipoMovimiento.RETIRO, monto, monto,
                "RETIRO", clave, null, AHORA);
        ReflectionTestUtils.setField(movimiento, "id", id);
        return movimiento;
    }
}
