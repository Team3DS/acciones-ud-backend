package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovimientoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Test
    void creaUnMovimientoConLosDatosNormalizados() {
        Movimiento movimiento = Movimiento.crear(3L, TipoMovimiento.DEPOSITO, new BigDecimal("100.005"),
                new BigDecimal("100.01"), "DEPOSITO", "DEP:clave", "Abono inicial", AHORA);

        assertThat(movimiento.getCuentaId()).isEqualTo(3L);
        assertThat(movimiento.getTipo()).isEqualTo(TipoMovimiento.DEPOSITO);
        assertThat(movimiento.getMonto()).isEqualByComparingTo("100.01");
        assertThat(movimiento.getSaldoPosterior()).isEqualByComparingTo("100.01");
        assertThat(movimiento.getReferencia()).isEqualTo("DEPOSITO");
        assertThat(movimiento.getIdempotencyKey()).isEqualTo("DEP:clave");
        assertThat(movimiento.getDescripcion()).isEqualTo("Abono inicial");
        assertThat(movimiento.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    void exigeCuentaTipoClaveEInstante() {
        assertThatThrownBy(() -> Movimiento.crear(null, TipoMovimiento.DEPOSITO, BigDecimal.ONE,
                BigDecimal.ZERO, null, "DEP:x", null, AHORA)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Movimiento.crear(1L, null, BigDecimal.ONE,
                BigDecimal.ZERO, null, "DEP:x", null, AHORA)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Movimiento.crear(1L, TipoMovimiento.DEPOSITO, BigDecimal.ONE,
                BigDecimal.ZERO, null, null, null, AHORA)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Movimiento.crear(1L, TipoMovimiento.DEPOSITO, BigDecimal.ONE,
                BigDecimal.ZERO, null, "DEP:x", null, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rechazaMontosNoPositivos() {
        assertThatThrownBy(() -> Movimiento.crear(1L, TipoMovimiento.DEPOSITO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, "DEP:x", null, AHORA)).isInstanceOf(MontoInvalidoException.class);
    }
}
