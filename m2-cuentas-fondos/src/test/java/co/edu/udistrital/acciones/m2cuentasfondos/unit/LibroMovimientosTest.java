package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoCreadoPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LibroMovimientosTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);

    @Test
    void registraElMovimientoYPublicaElEventoEnElOutbox() {
        MovimientoRepositoryPort movimientos = mock(MovimientoRepositoryPort.class);
        OutboxPort outbox = mock(OutboxPort.class);
        when(movimientos.save(any(Movimiento.class))).thenAnswer(invocacion -> {
            Movimiento movimiento = invocacion.getArgument(0);
            ReflectionTestUtils.setField(movimiento, "id", 55L);
            return movimiento;
        });
        LibroMovimientos libro = new LibroMovimientos(movimientos, outbox, CLOCK);
        Cuenta cuenta = Cuenta.crear(5L, AHORA);
        ReflectionTestUtils.setField(cuenta, "id", 10L);
        cuenta.abonar(new BigDecimal("80.00"), AHORA);

        libro.registrar(cuenta, TipoMovimiento.DEPOSITO, new BigDecimal("20.00"),
                "DEPOSITO", "DEP:x", "descripción");

        ArgumentCaptor<Movimiento> movimientoCaptor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(movimientoCaptor.capture());
        assertThat(movimientoCaptor.getValue().getCuentaId()).isEqualTo(10L);
        assertThat(movimientoCaptor.getValue().getSaldoPosterior()).isEqualByComparingTo("80.00");
        assertThat(movimientoCaptor.getValue().getCreadoEn()).isEqualTo(AHORA);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outbox).registrar(org.mockito.ArgumentMatchers.eq("Movimiento"),
                org.mockito.ArgumentMatchers.eq("55"),
                org.mockito.ArgumentMatchers.eq(RoutingKeys.MOVIMIENTO_CREADO),
                payloadCaptor.capture());
        MovimientoCreadoPayload payload = (MovimientoCreadoPayload) payloadCaptor.getValue();
        assertThat(payload.movementId()).isEqualTo(55L);
        assertThat(payload.userId()).isEqualTo(5L);
        assertThat(payload.type()).isEqualTo("DEPOSITO");
        assertThat(payload.amount()).isEqualByComparingTo("20.00");
        assertThat(payload.balance()).isEqualByComparingTo("80.00");
    }
}
