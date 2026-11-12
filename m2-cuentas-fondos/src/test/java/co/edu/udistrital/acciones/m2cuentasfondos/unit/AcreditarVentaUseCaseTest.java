package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenEjecutadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.AcreditarVentaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
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
class AcreditarVentaUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Mock
    private CuentaRepositoryPort cuentas;

    @Mock
    private MovimientoRepositoryPort movimientos;

    @Mock
    private OutboxPort outbox;

    private AcreditarVentaUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new AcreditarVentaUseCase(cuentas, movimientos, new LibroMovimientos(movimientos, outbox, CLOCK),
                CLOCK);
    }

    @Test
    void acreditaLaVentaComoMovimientoCreditoVenta() {
        Cuenta cuenta = Cuenta.crear(5L, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", 10L);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        when(cuentas.findByUserIdForUpdate(5L)).thenReturn(Optional.of(cuenta));
        when(movimientos.findByIdempotencyKey("VTA:orden-1")).thenReturn(Optional.empty());
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 101L);
            return movimiento;
        });

        casoDeUso.ejecutar(new OrdenEjecutadaPayload("orden-1", "ejecucion-1", List.of(),
                new BigDecimal("50.00"), "SELL", 5L));

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("150.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("150.00");
        verify(cuentas).crearSiNoExiste(eq(5L), any(Instant.class));
        verify(cuentas).save(cuenta);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(captor.capture());
        Movimiento movimiento = captor.getValue();
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.CREDITO_VENTA);
        assertThat(movimiento.getMonto()).isEqualByComparingTo("50.00");
        assertThat(movimiento.getSaldoPosterior()).isEqualByComparingTo("150.00");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("VTA:orden-1");
        assertThat(movimiento.getReferencia()).isEqualTo("CREDITO_VENTA");

        verify(outbox).registrar(eq("Movimiento"), eq("101"), eq(RoutingKeys.MOVIMIENTO_CREADO), any());
    }

    @Test
    void noAcreditaDosVecesLaMismaOrden() {
        Movimiento existente = Movimiento.crear(10L, TipoMovimiento.CREDITO_VENTA, new BigDecimal("50.00"),
                new BigDecimal("150.00"), "CREDITO_VENTA", "VTA:orden-1", "venta", AHORA);
        when(movimientos.findByIdempotencyKey("VTA:orden-1")).thenReturn(Optional.of(existente));

        casoDeUso.ejecutar(new OrdenEjecutadaPayload("orden-1", "ejecucion-1", List.of(),
                new BigDecimal("50.00"), "SELL", 5L));

        verifyNoInteractions(cuentas);
        verify(movimientos, never()).save(any(Movimiento.class));
        verifyNoInteractions(outbox);
    }

    @Test
    void rechazaUnMontoNoPositivo() {
        assertThatThrownBy(() -> casoDeUso.ejecutar(new OrdenEjecutadaPayload("orden-1", "ejecucion-1",
                List.of(), new BigDecimal("0.00"), "SELL", 5L)))
                .isInstanceOf(MontoInvalidoException.class);

        verifyNoInteractions(cuentas, outbox);
    }
}
