package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerCotizacionUseCaseImpl;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.CacheProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObtenerCotizacionUseCaseImplTest {

    @Mock
    AlpacaMarketDataPort alpacaPort;

    @Mock
    CacheCotizacionesPort cachePort;

    @Mock
    CatalogoClientPort catalogoPort;

    CacheProperties cacheProperties;
    ObtenerCotizacionUseCase useCase;

    @BeforeEach
    void setUp() {
        cacheProperties = new CacheProperties(1, 60);
        useCase = new ObtenerCotizacionUseCaseImpl(alpacaPort, cachePort, catalogoPort, cacheProperties);
    }

    @Test
    void obtenerCotizacionDesdeCache() {
        Cotizacion cached = new Cotizacion("AAPL", new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null);
        when(cachePort.get("AAPL")).thenReturn(Optional.of(cached));
        when(catalogoPort.validarSimbolo("AAPL")).thenReturn(true);

        Cotizacion result = useCase.obtenerCotizacion("AAPL");

        assertEquals(cached, result);
        verify(cachePort).get("AAPL");
        verify(alpacaPort, never()).obtenerCotizacion(anyString());
    }

    @Test
    void obtenerCotizacionDesdeAlpacaSiNoEnCache() {
        Cotizacion fresh = new Cotizacion("AAPL", new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null);
        when(cachePort.get("AAPL")).thenReturn(Optional.empty());
        when(catalogoPort.validarSimbolo("AAPL")).thenReturn(true);
        when(alpacaPort.obtenerCotizacion("AAPL")).thenReturn(fresh);

        Cotizacion result = useCase.obtenerCotizacion("AAPL");

        assertEquals(fresh, result);
        verify(cachePort).put(eq("AAPL"), eq(fresh), any(Duration.class));
    }

    @Test
    void simboloInvalidoLanzaExcepcion() {
        when(catalogoPort.validarSimbolo("INVALID")).thenReturn(false);

        assertThrows(co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException.class,
                () -> useCase.obtenerCotizacion("INVALID"));
    }

    @Test
    void obtenerMultiplesCotizaciones() {
        Cotizacion aapl = new Cotizacion("AAPL", new BigDecimal("150.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null);
        Cotizacion tsla = new Cotizacion("TSLA", new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), null, null, null, null, null, null);

        when(catalogoPort.validarSimbolo("AAPL")).thenReturn(true);
        when(catalogoPort.validarSimbolo("TSLA")).thenReturn(true);
        when(cachePort.get("AAPL")).thenReturn(Optional.empty()).thenReturn(Optional.of(aapl));
        when(cachePort.get("TSLA")).thenReturn(Optional.of(tsla));
        when(alpacaPort.obtenerCotizaciones(List.of("AAPL"))).thenReturn(List.of(aapl));

        List<Cotizacion> result = useCase.obtenerCotizaciones(List.of("AAPL", "TSLA"));

        assertEquals(2, result.size());
        verify(alpacaPort).obtenerCotizaciones(List.of("AAPL"));
    }
}