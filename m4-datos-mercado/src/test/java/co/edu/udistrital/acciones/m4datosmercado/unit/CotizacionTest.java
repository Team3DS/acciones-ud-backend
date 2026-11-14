package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CotizacionTest {

    @Test
    void crearCotizacionValida() {
        Cotizacion cotizacion = new Cotizacion(
                "AAPL",
                new BigDecimal("150.25"),
                new BigDecimal("1.25"),
                new BigDecimal("0.84"),
                1000000L,
                Instant.now(),
                new BigDecimal("150.20"),
                new BigDecimal("150.30"),
                new BigDecimal("151.00"),
                new BigDecimal("149.50"),
                new BigDecimal("149.80"),
                new BigDecimal("149.00")
        );

        assertEquals("AAPL", cotizacion.symbol());
        assertEquals(new BigDecimal("150.25"), cotizacion.price());
        assertEquals(new BigDecimal("1.25"), cotizacion.change());
        assertEquals(new BigDecimal("0.84"), cotizacion.changePct());
    }

    @Test
    void crearCotizacionBasica() {
        Cotizacion cotizacion = Cotizacion.basica("TSLA", new BigDecimal("250.00"), Instant.now());

        assertEquals("TSLA", cotizacion.symbol());
        assertEquals(new BigDecimal("250.00"), cotizacion.price());
        assertEquals(BigDecimal.ZERO, cotizacion.change());
        assertEquals(BigDecimal.ZERO, cotizacion.changePct());
        assertEquals(0L, cotizacion.volume());
    }

    @Test
    void simboloNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cotizacion(null, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null));
    }

    @Test
    void simboloVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cotizacion("  ", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null));
    }

    @Test
    void precioNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cotizacion("AAPL", null, BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null));
    }

    @Test
    void timestampNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                new Cotizacion("AAPL", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, 0L, null, null, null, null, null, null, null));
    }
}