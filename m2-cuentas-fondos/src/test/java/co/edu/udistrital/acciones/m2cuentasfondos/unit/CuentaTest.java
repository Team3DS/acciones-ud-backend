package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EstadoCuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.CuentaNoOperableException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
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

    @Test
    void cargarReduceElContableYElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);

        cuenta.cargar(new BigDecimal("200.00"), AHORA.plusSeconds(5));

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("800.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("800.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA.plusSeconds(5));
    }

    @Test
    void permiteCargarTodoElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("150.00"), AHORA);

        cuenta.cargar(new BigDecimal("150.00"), AHORA);

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("0.00");
    }

    @Test
    void noPermiteCargarMasQueElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);

        assertThatThrownBy(() -> cuenta.cargar(new BigDecimal("100.01"), AHORA))
                .isInstanceOf(FondosInsuficientesException.class)
                .hasMessageContaining("100.00");

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("100.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("100.00");
    }

    @Test
    void noPermiteCargarMontosInvalidos() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);

        assertThatThrownBy(() -> cuenta.cargar(null, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.cargar(BigDecimal.ZERO, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.cargar(new BigDecimal("-1.00"), AHORA))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void noPermiteCargarSiLaCuentaNoEstaActiva() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        assertThatThrownBy(() -> cuenta.cargar(new BigDecimal("10.00"), AHORA))
                .isInstanceOf(CuentaNoOperableException.class)
                .hasMessageContaining("BLOQUEADA");
    }

    @Test
    void reservarMueveElDisponibleAlReservadoSinTocarElContable() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);

        cuenta.reservar(new BigDecimal("300.00"), AHORA.plusSeconds(10));

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("700.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("300.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA.plusSeconds(10));
    }

    @Test
    void permiteReservarTodoElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("500.00"), AHORA);

        cuenta.reservar(new BigDecimal("500.00"), AHORA);

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("500.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("500.00");
    }

    @Test
    void noPermiteReservarMasQueElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("500.00"), AHORA);

        assertThatThrownBy(() -> cuenta.reservar(new BigDecimal("500.01"), AHORA))
                .isInstanceOf(FondosInsuficientesException.class)
                .hasMessageContaining("500.00");

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("500.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("500.00");
    }

    @Test
    void noPermiteReservarMontosInvalidos() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);

        assertThatThrownBy(() -> cuenta.reservar(null, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.reservar(BigDecimal.ZERO, AHORA)).isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> cuenta.reservar(new BigDecimal("-1.00"), AHORA))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void noPermiteReservarSiLaCuentaNoEstaActiva() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        assertThatThrownBy(() -> cuenta.reservar(new BigDecimal("10.00"), AHORA))
                .isInstanceOf(CuentaNoOperableException.class)
                .hasMessageContaining("BLOQUEADA");
    }

    @Test
    void confirmarLaReservaExactaDescargaElContableYElReservado() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);

        cuenta.confirmarReserva(new BigDecimal("300.00"), new BigDecimal("300.00"), AHORA.plusSeconds(30));

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("700.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("700.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA.plusSeconds(30));
    }

    @Test
    void confirmarParcialDevuelveElSobranteAlDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);

        cuenta.confirmarReserva(new BigDecimal("300.00"), new BigDecimal("290.50"), AHORA);

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("709.50");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("709.50");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
    }

    @Test
    void confirmarMayorQueLoReservadoCubreLaDiferenciaConElDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);

        cuenta.confirmarReserva(new BigDecimal("300.00"), new BigDecimal("350.00"), AHORA);

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("650.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("650.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
    }

    @Test
    void noConfirmaSiElDisponibleNoCubreLaDiferencia() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("500.00"), AHORA);
        cuenta.reservar(new BigDecimal("500.00"), AHORA);

        assertThatThrownBy(() -> cuenta.confirmarReserva(new BigDecimal("500.00"), new BigDecimal("600.00"), AHORA))
                .isInstanceOf(FondosInsuficientesException.class)
                .hasMessageContaining("100.00");

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("500.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("500.00");
    }

    @Test
    void liberarLaReservaDevuelveElMontoAlDisponible() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("300.00"), AHORA);

        cuenta.liberarReserva(new BigDecimal("300.00"), AHORA.plusSeconds(30));

        assertThat(cuenta.getSaldoContable()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("1000.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("0.00");
        assertThat(cuenta.getActualizadoEn()).isEqualTo(AHORA.plusSeconds(30));
    }

    @Test
    void noLiberaMasQueLoReservado() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("1000.00"), AHORA);
        cuenta.reservar(new BigDecimal("100.00"), AHORA);

        assertThatThrownBy(() -> cuenta.liberarReserva(new BigDecimal("100.01"), AHORA))
                .isInstanceOf(TransicionReservaInvalidaException.class);

        assertThat(cuenta.getSaldoDisponible()).isEqualByComparingTo("900.00");
        assertThat(cuenta.getSaldoReservado()).isEqualByComparingTo("100.00");
    }

    @Test
    void noConfirmaNiLiberaSiLaCuentaNoEstaActiva() {
        Cuenta cuenta = Cuenta.crear(7L, AHORA);
        cuenta.abonar(new BigDecimal("100.00"), AHORA);
        cuenta.reservar(new BigDecimal("10.00"), AHORA);
        ReflectionTestUtils.setField(cuenta, "estado", EstadoCuenta.BLOQUEADA);

        assertThatThrownBy(() -> cuenta.confirmarReserva(new BigDecimal("10.00"), new BigDecimal("10.00"), AHORA))
                .isInstanceOf(CuentaNoOperableException.class);
        assertThatThrownBy(() -> cuenta.liberarReserva(new BigDecimal("10.00"), AHORA))
                .isInstanceOf(CuentaNoOperableException.class);
    }
}
