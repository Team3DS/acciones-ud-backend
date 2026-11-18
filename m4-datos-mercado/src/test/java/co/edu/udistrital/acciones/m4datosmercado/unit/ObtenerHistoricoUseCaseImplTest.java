package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException;
import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerHistoricoUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.HistoricoRepositoryPort;
import co.edu.udistrital.acciones.m4datosmercado.application.usecase.ObtenerHistoricoUseCaseImpl;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObtenerHistoricoUseCaseImplTest {

    @Mock
    HistoricoRepositoryPort historicoPort;

    @Mock
    AlpacaMarketDataPort alpacaPort;

    @Mock
    CatalogoClientPort catalogoPort;

    ObtenerHistoricoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ObtenerHistoricoUseCaseImpl(historicoPort, alpacaPort, catalogoPort);
    }

    @Test
    void obtenerHistoricoDesdeCache() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        HistoricoCotizacion h = new HistoricoCotizacion("AAPL", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 100L, from, Intervalo.M1);

        when(catalogoPort.validarSimbolo("AAPL")).thenReturn(true);
        when(historicoPort.findBySymbolAndIntervaloAndTimestampBetween("AAPL", Intervalo.M1, from, to)).thenReturn(List.of(h));

        List<HistoricoCotizacion> result = useCase.obtenerHistorico("AAPL", from, to, Intervalo.M1);

        assertEquals(1, result.size());
        verify(alpacaPort, never()).obtenerHistorico(anyString(), any(), any(), any());
    }

    @Test
    void obtenerHistoricoDesdeAlpacaSiNoEnCache() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        HistoricoCotizacion h = new HistoricoCotizacion("AAPL", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 100L, from, Intervalo.M1);

        when(catalogoPort.validarSimbolo("AAPL")).thenReturn(true);
        when(historicoPort.findBySymbolAndIntervaloAndTimestampBetween("AAPL", Intervalo.M1, from, to)).thenReturn(List.of());
        when(alpacaPort.obtenerHistorico("AAPL", from, to, Intervalo.M1)).thenReturn(List.of(h));

        List<HistoricoCotizacion> result = useCase.obtenerHistorico("AAPL", from, to, Intervalo.M1);

        assertEquals(1, result.size());
        verify(historicoPort).saveAll(List.of(h));
    }

    @Test
    void simboloInvalidoLanzaExcepcion() {
        when(catalogoPort.validarSimbolo("INVALID")).thenReturn(false);

        assertThrows(SimboloInvalidoException.class, () ->
                useCase.obtenerHistorico("INVALID", Instant.now().minusSeconds(3600), Instant.now(), Intervalo.M1));
    }
}