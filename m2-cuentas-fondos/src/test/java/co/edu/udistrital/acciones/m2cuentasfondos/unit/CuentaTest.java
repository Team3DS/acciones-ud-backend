package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CuentaTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Test
    void creaUnaCuentaActivaConSaldosEnCero() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);

        assertThat(cuenta.getUserId()).isEqualTo(7L);
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuenta.ACTIVA);
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getCreadoEn()).isEqualTo(AHORA);
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    void abonarAumentaElContableYElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);

        cuenta.abonar(new BigDecimal("1000.00"), AHORA.plusSeconds(5));
        cuenta.abonar(new BigDecimal("250.555"), AHORA.plusSeconds(10));

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("1250.56");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1250.56");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA.plusSeconds(10));
    }

    @Test
    void noPermiteAbonarMontosInvalidos() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);

        assertThatThrownBy(() -> cuenta.abonar(null, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.abonar(BigDecimal.ZERO, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.abonar(new BigDecimal("-5.00"), AHORA))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void noPermiteAbonarSiLaCuentaNoEstaActiva() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        assertThatThrownBy(() -> cuenta.abonar(new BigDecimal("10.00"), AHORA))
                .isInstanceOf(CuentaNoOperableException.class)
                .hasMessageContaining("BLOQUEADA");
    }

    @Test
    void verificarOperableFallaParaCuentaCerrada() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.CERRADA);

        assertThatThrownBy(cuenta::verificarOperable)
                .isInstanceOf(CuentaNoOperableException.class)
                .hasMessageContaining("CERRADA");
    }
}
