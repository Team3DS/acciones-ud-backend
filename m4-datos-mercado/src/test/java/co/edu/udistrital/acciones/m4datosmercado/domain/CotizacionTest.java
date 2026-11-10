package co.edu.udistrital.acciones.m4datosmercado.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class CotizacionTest {
    @Test
    void cotizacionRecordEquality() {
        Cotizacion c1 = new Cotizacion("AAPL", BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.ONE, 10L, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        Cotizacion c2 = new Cotizacion("AAPL", BigDecimal.valueOf(100), BigDecimal.ONE, BigDecimal.ONE, 10L, c1.timestamp(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        assertEquals(c1.symbol(), c2.symbol());
        assertEquals(c1.price(), c2.price());
    }
}
