package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.WebhooksProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhooksPropertiesTest {

    @Test
    void aplicaLosValoresPorDefecto() {
        WebhooksProperties propiedades = new WebhooksProperties(true, null, 0, 0, null, null);

        assertThat(propiedades.fixedDelay()).isEqualTo(Duration.ofSeconds(10));
        assertThat(propiedades.lote()).isEqualTo(50);
        assertThat(propiedades.maxIntentos()).isEqualTo(10);
        assertThat(propiedades.backoffBase()).isEqualTo(Duration.ofSeconds(30));
        assertThat(propiedades.backoffMax()).isEqualTo(Duration.ofHours(6));
    }

    @Test
    void rechazaUnaDemoraNoPositiva() {
        assertThatThrownBy(() -> new WebhooksProperties(true, Duration.ZERO, 10, 3,
                Duration.ofSeconds(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fixed-delay");
    }

    @Test
    void rechazaUnBackoffBaseNoPositivo() {
        assertThatThrownBy(() -> new WebhooksProperties(true, Duration.ofSeconds(1), 10, 3,
                Duration.ZERO, Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("backoff-base");
    }

    @Test
    void rechazaUnBackoffMaximoMenorQueElBase() {
        assertThatThrownBy(() -> new WebhooksProperties(true, Duration.ofSeconds(1), 10, 3,
                Duration.ofHours(2), Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("backoff-max");
    }
}
