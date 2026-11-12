package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.MonetaryUtil;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.MontoInvalidoException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonetaryUtilTest {

    @Test
    void normalizaALaEscalaDeDosDecimales() {
        assertThat(MonetaryUtil.normalizar(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(MonetaryUtil.normalizar(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
        assertThat(MonetaryUtil.normalizar(new BigDecimal("7"))).isEqualByComparingTo("7.00");
    }

    @Test
    void elCeroTieneEscalaDos() {
        assertThat(MonetaryUtil.cero()).isEqualByComparingTo("0.00");
        assertThat(MonetaryUtil.cero().scale()).isEqualTo(2);
    }

    @Test
    void rechazaUnMontoNulo() {
        assertThatThrownBy(() -> MonetaryUtil.normalizar(null))
                .isInstanceOf(MontoInvalidoException.class)
                .hasMessageContaining("obligatorio");
    }

    @Test
    void rechazaMontosNoPositivos() {
        assertThatThrownBy(() -> MonetaryUtil.validarPositivo(BigDecimal.ZERO))
                .isInstanceOf(MontoInvalidoException.class);
        assertThatThrownBy(() -> MonetaryUtil.validarPositivo(new BigDecimal("-0.01")))
                .isInstanceOf(MontoInvalidoException.class);
    }

    @Test
    void aceptaUnMontoPositivoYLoNormaliza() {
        assertThat(MonetaryUtil.validarPositivo(new BigDecimal("0.015"))).isEqualByComparingTo("0.02");
    }
}
