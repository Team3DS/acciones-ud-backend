package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class ObtenerCotizacionUseCase {
    private final AlpacaMarketDataPort alpaca;
    private final CacheCotizacionesPort cache;
    private final CatalogoClientPort catalogo;

    public ObtenerCotizacionUseCase(AlpacaMarketDataPort alpaca, CacheCotizacionesPort cache, CatalogoClientPort catalogo) {
        this.alpaca = alpaca;
        this.cache = cache;
        this.catalogo = catalogo;
    }

    public Cotizacion execute(String symbol) {
        if (!catalogo.simboloExiste(symbol)) {
            throw new IllegalArgumentException("Simbolo no valido: " + symbol);
        }
        Optional<Cotizacion> cached = cache.get(symbol);
        if (cached.isPresent()) return cached.get();
        Cotizacion cot = alpaca.obtenerCotizacion(symbol);
        cache.put(symbol, cot, Duration.ofSeconds(1));
        return cot;
    }

    public List<Cotizacion> executeMultiple(List<String> symbols) {
        List<String> validos = catalogo.validarSimbolos(symbols);
        return alpaca.obtenerCotizaciones(validos);
    }
}
