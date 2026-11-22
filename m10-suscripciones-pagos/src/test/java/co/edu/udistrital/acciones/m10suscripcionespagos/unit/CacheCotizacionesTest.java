package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest.CacheCotizaciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.ClientesHttpProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.RelojControlado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CacheCotizacionesTest {

    private static final Instant INICIO = Instant.parse("2026-11-22T00:00:00Z");
    private static final Cotizacion COTIZACION = new Cotizacion("AAPL", new BigDecimal("200.00"),
            new BigDecimal("1.00"), new BigDecimal("0.50"), INICIO);

    @Test
    void devuelveLaCotizacionAlmacenadaAntesDeQueExpire() {
        RelojControlado reloj = new RelojControlado(INICIO);
        CacheCotizaciones cache = new CacheCotizaciones(propiedades(), reloj);
        cache.guardar(COTIZACION);

        reloj.avanzar(4);

        assertThat(cache.obtener("AAPL")).contains(COTIZACION);
    }

    @Test
    void descartaLaCotizacionDespuesDelTiempoDeVida() {
        RelojControlado reloj = new RelojControlado(INICIO);
        CacheCotizaciones cache = new CacheCotizaciones(propiedades(), reloj);
        cache.guardar(COTIZACION);

        reloj.avanzar(5);

        assertThat(cache.obtener("AAPL")).isEmpty();
    }

    private static ClientesHttpProperties propiedades() {
        return new ClientesHttpProperties("http://m3", "http://m4", 5, 5);
    }
}
