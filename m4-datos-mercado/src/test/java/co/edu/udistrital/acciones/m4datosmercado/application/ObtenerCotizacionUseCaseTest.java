package co.edu.udistrital.acciones.m4datosmercado.application;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObtenerCotizacionUseCaseTest {
    @Test
    void execute_cacheHit_returnsCached() {
        AlpacaMarketDataPort alpaca = mock(AlpacaMarketDataPort.class);
        CacheCotizacionesPort cache = mock(CacheCotizacionesPort.class);
        CatalogoClientPort catalogo = mock(CatalogoClientPort.class);
        when(catalogo.simboloExiste("AAPL")).thenReturn(true);
        Cotizacion cached = new Cotizacion("AAPL", BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, 1L, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(cache.get("AAPL")).thenReturn(Optional.of(cached));

        ObtenerCotizacionUseCase uc = new ObtenerCotizacionUseCase(alpaca, cache, catalogo);
        Cotizacion result = uc.execute("AAPL");

        assertSame(cached, result);
        verify(alpaca, never()).obtenerCotizacion(anyString());
    }

    @Test
    void execute_cacheMiss_validSymbol_callsAlpacaAndCaches() {
        AlpacaMarketDataPort alpaca = mock(AlpacaMarketDataPort.class);
        CacheCotizacionesPort cache = mock(CacheCotizacionesPort.class);
        CatalogoClientPort catalogo = mock(CatalogoClientPort.class);
        when(catalogo.simboloExiste("TSLA")).thenReturn(true);
        when(cache.get("TSLA")).thenReturn(Optional.empty());
        Cotizacion fromAlpaca = new Cotizacion("TSLA", BigDecimal.valueOf(800), BigDecimal.ZERO, BigDecimal.ZERO, 2L, Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
        when(alpaca.obtenerCotizacion("TSLA")).thenReturn(fromAlpaca);

        ObtenerCotizacionUseCase uc = new ObtenerCotizacionUseCase(alpaca, cache, catalogo);
        Cotizacion result = uc.execute("TSLA");

        assertSame(fromAlpaca, result);
        verify(cache).put(eq("TSLA"), eq(fromAlpaca), any());
    }

    @Test
    void execute_invalidSymbol_throws() {
        AlpacaMarketDataPort alpaca = mock(AlpacaMarketDataPort.class);
        CacheCotizacionesPort cache = mock(CacheCotizacionesPort.class);
        CatalogoClientPort catalogo = mock(CatalogoClientPort.class);
        when(catalogo.simboloExiste("XXX")).thenReturn(false);

        ObtenerCotizacionUseCase uc = new ObtenerCotizacionUseCase(alpaca, cache, catalogo);
        assertThrows(IllegalArgumentException.class, () -> uc.execute("XXX"));
    }
}
