package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.DepositoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.RegistrarDepositoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
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
class RegistrarDepositoUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    private RegistrarDepositoUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new RegistrarDepositoUseCase(cuentas, movimientos,
                new LibroMovimientos(movimientos, CLOCK), CLOCK);
    }

    @Test
    void registraElDepositoYGuardaElMovimiento() {
        Cuenta cuenta = cuenta(10L, 5L);
        when(movimientos.findByIdempotencyKey("DEP:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 99L);
            return movimiento;
        });

        DepositoResponse respuesta = casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("1000.00"), "Abono inicial"),
                "clave-1", new Actor(1L, true));

        assertThat(respuesta.movementId()).isEqualTo(99L);
        assertThat(respuesta.userId()).isEqualTo(5L);
        assertThat(respuesta.amount()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.available()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("0.00");
        assertThat(respuesta.total()).isEqualByComparingTo("1000.00");
        assertThat(respuesta.currency()).isEqualTo("USD");
        verify(cuentas).crearSiNoExiste(5L, AHORA);
        verify(cuentas).save(cuenta);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimiento.DEPOSITO);
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("DEP:clave-1");
        assertThat(captor.getValue().getSaldoPosterior()).isEqualByComparingTo("1000.00");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Abono inicial");
    }

    @Test
    void repiteLaRespuestaCuandoLaClaveYaExisteConLosMismosDatos() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("100.00"), "DEP:clave-1");
        when(movimientos.findByIdempotencyKey("DEP:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        DepositoResponse respuesta = casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(1L, true));

        assertThat(respuesta.movementId()).isEqualTo(77L);
        assertThat(respuesta.available()).isEqualByComparingTo("100.00");
        verify(cuentas, never()).crearSiNoExiste(any(), any());
        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void detectaLaRepeticionDespuesDeBloquearLaCuenta() {
        Cuenta cuenta = cuenta(10L, 5L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("100.00"), "DEP:clave-1");
        when(movimientos.findByIdempotencyKey("DEP:clave-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existente));
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        DepositoResponse respuesta = casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(1L, true));

        assertThat(respuesta.movementId()).isEqualTo(77L);
        verify(cuentas, never()).save(any());
        verify(movimientos, never()).save(any());
    }

    @Test
    void rechazaLaMismaClaveConOtroMonto() {
        Cuenta cuenta = cuenta(10L, 5L);
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("50.00"), "DEP:clave-1");
        when(movimientos.findByIdempotencyKey("DEP:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(1L, true)))
                .isInstanceOf(ConflictoIdempotenciaException.class);

        verify(cuentas, never()).crearSiNoExiste(any(), any());
    }

    @Test
    void rechazaLaClaveUsadaPorOtroUsuario() {
        Movimiento existente = movimiento(77L, 10L, new BigDecimal("100.00"), "DEP:clave-1");
        when(movimientos.findByIdempotencyKey("DEP:clave-1")).thenReturn(Optional.of(existente));
        when(cuentas.findByUserId(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new DepositoRequest(6L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(1L, true)))
                .isInstanceOf(ConflictoIdempotenciaException.class);
    }

    @Test
    void noAbonaSiLaCuentaEstaBloqueada() {
        Cuenta cuenta = cuenta(10L, 5L);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);
        when(movimientos.findByIdempotencyKey("DEP:clave-1")).thenReturn(Optional.empty());
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(1L, true)))
                .isInstanceOf(CuentaNoOperableException.class);

        verify(cuentas, never()).save(any());
    }

    @Test
    void soloUnAdministradorPuedeRegistrarDepositos() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(
                new DepositoRequest(5L, new BigDecimal("100.00"), null),
                "clave-1", new Actor(5L, false)))
                .isInstanceOf(AccesoDenegadoException.class);

        verifyNoInteractions(cuentas, movimientos);
    }

    private Cuenta cuenta(Long id, Long userId) {
        Cuenta cuenta = Cuenta.crear(userId, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", id);
        return cuenta;
    }

    private Movimiento movimiento(Long id, Long cuentaId, BigDecimal monto, String clave) {
        Movimiento movimiento = Movimiento.crear(cuentaId, TipoMovimiento.DEPOSITO, monto, monto,
                "DEPOSITO", clave, null, AHORA);
        ReflectionTestUtils.setField(movimiento, "id", id);
        return movimiento;
    }
}
