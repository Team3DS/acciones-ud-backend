package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SaldoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConsultarSaldoUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarSaldoUseCaseTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");

    @Mock
    private CuentaRepositoryPort cuentas;

    @Test
    void devuelveCerosSiElUsuarioNoTieneCuenta() {
        when(cuentas.findByUserId(5L)).thenReturn(Optional.empty());
        ConsultarSaldoUseCase casoDeUso = new ConsultarSaldoUseCase(cuentas);

        SaldoResponse respuesta = casoDeUso.ejecutar(5L, new Actor(5L, false));

        assertThat(respuesta.userId()).isEqualTo(5L);
        assertThat(respuesta.available()).isEqualByComparingTo("0.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("0.00");
        assertThat(respuesta.total()).isEqualByComparingTo("0.00");
        assertThat(respuesta.currency()).isEqualTo("USD");
    }

    @Test
    void devuelveLosSaldosDelDueno() {
        Cuenta cuenta = Cuenta.crear(5L, AHORA);
        cuenta.abonar(new BigDecimal("500.00"), AHORA);
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));
        ConsultarSaldoUseCase casoDeUso = new ConsultarSaldoUseCase(cuentas);

        SaldoResponse respuesta = casoDeUso.ejecutar(5L, new Actor(5L, false));

        assertThat(respuesta.available()).isEqualByComparingTo("500.00");
        assertThat(respuesta.reserved()).isEqualByComparingTo("0.00");
        assertThat(respuesta.total()).isEqualByComparingTo("500.00");
    }

    @Test
    void unAdministradorPuedeConsultarCuentasAjenas() {
        Cuenta cuenta = Cuenta.crear(5L, AHORA);
        cuenta.abonar(new BigDecimal("75.00"), AHORA);
        when(cuentas.findByUserId(5L)).thenReturn(Optional.of(cuenta));
        ConsultarSaldoUseCase casoDeUso = new ConsultarSaldoUseCase(cuentas);

        SaldoResponse respuesta = casoDeUso.ejecutar(5L, new Actor(1L, true));

        assertThat(respuesta.available()).isEqualByComparingTo("75.00");
    }

    @Test
    void unUsuarioAjenoNoPuedeConsultarElSaldo() {
        ConsultarSaldoUseCase casoDeUso = new ConsultarSaldoUseCase(cuentas);

        assertThatThrownBy(() -> casoDeUso.ejecutar(5L, new Actor(9L, false)))
                .isInstanceOf(AccesoDenegadoException.class);
    }
}
