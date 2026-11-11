package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoReserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservaTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Test
    void creaUnaReservaPendienteConElMontoNormalizado() {
        Reserva reserva = Reserva.crear(10L, "orden-1", new BigDecimal("300.005"), "RES:clave-1", AHORA);

        assertThat(reserva.getCuentaId()).isEqualTo(10L);
        assertThat(reserva.getOrderId()).isEqualTo("orden-1");
        assertThat(reserva.getMonto()).isEqualByComparingTo("300.01");
        assertThat(reserva.getMonto().scale()).isEqualTo(2);
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.PENDING);
        assertThat(reserva.getIdempotencyKey()).isEqualTo("RES:clave-1");
        assertThat(reserva.getCreadoEn()).isEqualTo(AHORA);
        assertThat(reserva.getExecutionId()).isNull();
        assertThat(reserva.getMotivoLiberacion()).isNull();
        assertThat(reserva.getConfirmadoEn()).isNull();
        assertThat(reserva.getLiberadoEn()).isNull();
    }

    @Test
    void noPermiteMontosInvalidos() {
        assertThatThrownBy(() -> Reserva.crear(10L, "orden-1", null, "RES:clave-1", AHORA))
                .isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> Reserva.crear(10L, "orden-1", BigDecimal.ZERO, "RES:clave-1", AHORA))
                .isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> Reserva.crear(10L, "orden-1", new BigDecimal("-1.00"), "RES:clave-1", AHORA))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void exigeCuentaOrdenClaveEInstante() {
        BigDecimal monto = new BigDecimal("10.00");

        assertThatThrownBy(() -> Reserva.crear(null, "orden-1", monto, "RES:clave-1", AHORA))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reserva.crear(10L, null, monto, "RES:clave-1", AHORA))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reserva.crear(10L, "orden-1", monto, null, AHORA))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Reserva.crear(10L, "orden-1", monto, "RES:clave-1", null))
                .isInstanceOf(NullPointerException.class);
    }
}
