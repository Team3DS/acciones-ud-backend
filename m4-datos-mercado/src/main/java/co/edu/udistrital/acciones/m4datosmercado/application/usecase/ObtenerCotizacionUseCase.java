package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Simbolo requerido");
        }
        if (!catalogo.simboloExiste(symbol)) {
            throw new IllegalArgumentException("Simbolo no valido: " + symbol);
        }
        Optional<Cotizacion> cached = cache.get(symbol);
        if (cached.isPresent()) return cached.get();
        Cotizacion cot = alpaca.obtenerCotizacion(symbol);
        if (cot == null || cot.price() == null || cot.price().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cotizacion invalida para simbolo: " + symbol);
        }
        cache.put(symbol, cot, Duration.ofSeconds(1));
        return cot;
    }

    public List<Cotizacion> executeMultiple(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }
        List<String> validos = catalogo.validarSimbolos(symbols);
        if (validos.isEmpty()) return List.of();
        return alpaca.obtenerCotizaciones(validos);
    }

    public Set<String> obtenerSimbolosValidos() {
        if (catalogo instanceof co.edu.udistrital.acciones.m4datosmercado.infrastructure.adapter.out.CatalogoRestClient) {
            return Set.of();
        }
        return Set.of();
    }
}
