package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.ValoracionProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValoracionPropertiesTest {

    @Test
    void conservaLosValoresConfigurados() {
        ValoracionProperties propiedades = new ValoracionProperties(true, 30, true, 200);

        assertThat(propiedades.schedulerEnabled()).isTrue();
        assertThat(propiedades.intervalSeconds()).isEqualTo(30);
        assertThat(propiedades.soloMercadoAbierto()).isTrue();
        assertThat(propiedades.lote()).isEqualTo(200);
    }

    @Test
    void rechazaUnIntervaloNoPositivo() {
        assertThatThrownBy(() -> new ValoracionProperties(true, 0, true, 200))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("interval-seconds");
    }

    @Test
    void rechazaUnLoteNoPositivo() {
        assertThatThrownBy(() -> new ValoracionProperties(true, 30, true, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lote");
    }
}
