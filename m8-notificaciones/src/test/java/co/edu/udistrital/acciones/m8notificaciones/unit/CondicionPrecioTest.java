package co.edu.udistrital.acciones.m8notificaciones.unit;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udistrital.acciones.m8notificaciones.domain.CondicionPrecio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

class CondicionPrecioTest {
    @ParameterizedTest
    @CsvSource({
        "SUPERA,101,100,true",
        "SUPERA,100,100,false",
        "SUPERA,99,100,false",
        "CAE_BAJO,99,100,true",
        "CAE_BAJO,100,100,false",
        "CAE_BAJO,101,100,false",
        "SUPERA,0,100,false",
        "CAE_BAJO,-1,100,false",
        "SUPERA,10,0,false"
    })
    void evaluaUmbralesSinDispararEnIgualdad(
            CondicionPrecio condition, String price, String target, boolean expected) {
        assertThat(condition.cumple(new BigDecimal(price), new BigDecimal(target)))
                .isEqualTo(expected);
    }

    @Test
    void descartaPreciosAusentes() {
        assertThat(CondicionPrecio.SUPERA.cumple(null, BigDecimal.ONE)).isFalse();
        assertThat(CondicionPrecio.CAE_BAJO.cumple(BigDecimal.ONE, null)).isFalse();
    }
}
