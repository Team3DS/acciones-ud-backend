package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado.CacheCotizaciones;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.M4ClientProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CacheCotizacionesTest {

    @Test
    void entregaUnaCotizacionHastaQueExpira() {
        RelojMutable clock = new RelojMutable(Instant.parse("2026-03-01T10:00:00Z"));
        CacheCotizaciones cache = new CacheCotizaciones(new M4ClientProperties("http://m4", 5, 5), clock);
        CotizacionMercado cotizacion = new CotizacionMercado("AAPL", new BigDecimal("200.00"), clock.instant());

        assertThat(cache.obtener("AAPL")).isEmpty();

        cache.guardar(cotizacion);

        assertThat(cache.obtener("AAPL")).contains(cotizacion);
        clock.avanzarSegundos(5);
        assertThat(cache.obtener("AAPL")).isEmpty();
    }

    private static final class RelojMutable extends Clock {
        private Instant instant;

        private RelojMutable(Instant instant) {
            this.instant = instant;
        }

        private void avanzarSegundos(long segundos) {
            instant = instant.plusSeconds(segundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
