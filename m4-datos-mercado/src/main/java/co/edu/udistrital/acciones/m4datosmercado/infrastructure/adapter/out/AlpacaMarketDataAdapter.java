package co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class AlpacaMarketDataAdapter implements AlpacaMarketDataPort {
    @Override
    public Cotizacion obtenerCotizacion(String symbol) {
        return new Cotizacion(symbol, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, 0L, Instant.now(), BigDecimal.valueOf(99.9), BigDecimal.valueOf(100.1), BigDecimal.valueOf(101), BigDecimal.valueOf(99), BigDecimal.valueOf(100), BigDecimal.valueOf(99));
    }

    @Override
    public List<Cotizacion> obtenerCotizaciones(List<String> symbols) {
        return symbols.stream().map(this::obtenerCotizacion).toList();
    }

    @Override
    public List<HistoricoCotizacion> obtenerHistorico(String symbol, Instant from, Instant to, Intervalo intervalo) {
        return List.of();
    }
}
