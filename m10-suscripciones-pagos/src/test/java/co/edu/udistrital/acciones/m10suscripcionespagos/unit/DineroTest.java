package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Dinero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

    @Test
    void convierteCentavosAdosDecimales() {
        Dinero dinero = Dinero.desdeCentavos(1200);

        assertThat(dinero.monto()).isEqualByComparingTo("12.00");
        assertThat(dinero.monto().scale()).isEqualTo(2);
        assertThat(dinero.moneda()).isEqualTo("USD");
    }

    @Test
    void normalizaConHalfUpHaciaArriba() {
        Dinero dinero = Dinero.de(new BigDecimal("12.345"), "usd");

        assertThat(dinero.monto()).isEqualByComparingTo("12.35");
        assertThat(dinero.moneda()).isEqualTo("USD");
    }

    @Test
    void normalizaConHalfUpHaciaAbajo() {
        Dinero dinero = Dinero.de(new BigDecimal("12.344"), "USD");

        assertThat(dinero.monto()).isEqualByComparingTo("12.34");
    }

    @Test
    void calculaLosCentavosDeUnMonto() {
        assertThat(Dinero.de(new BigDecimal("120.00"), "USD").centavos()).isEqualTo(12000L);
        assertThat(Dinero.desdeCentavos(1).centavos()).isEqualTo(1L);
    }

    @Test
    void rechazaUnMontoNegativo() {
        assertThatThrownBy(() -> Dinero.de(new BigDecimal("-0.01"), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void rechazaCentavosNegativos() {
        assertThatThrownBy(() -> Dinero.desdeCentavos(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativos");
    }

    @Test
    void rechazaUnaMonedaQueNoTieneTresCaracteres() {
        assertThatThrownBy(() -> Dinero.de(BigDecimal.ONE, "US"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 caracteres");
    }
}
