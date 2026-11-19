package co.edu.udistrital.acciones.m4datosmercado.application.usecase;

import co.edu.udistrital.acciones.m4datosmercado.application.port.in.ObtenerCotizacionUseCase;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.AlpacaMarketDataPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ObtenerCotizacionUseCaseImpl implements ObtenerCotizacionUseCase {

    private final AlpacaMarketDataPort alpacaPort;
    private final CacheCotizacionesPort cachePort;
    private final CatalogoClientPort catalogoPort;
    private final Duration quoteTtl;
    private final Duration snapshotTtl;

    public ObtenerCotizacionUseCaseImpl(AlpacaMarketDataPort alpacaPort,
                                        CacheCotizacionesPort cachePort,
                                        CatalogoClientPort catalogoPort,
                                        co.edu.udistrital.acciones.m4datosmercado.infrastructure.config.CacheProperties cacheProperties) {
        this.alpacaPort = alpacaPort;
        this.cachePort = cachePort;
        this.catalogoPort = catalogoPort;
        this.quoteTtl = Duration.ofSeconds(cacheProperties.quoteTtlSeconds());
        this.snapshotTtl = Duration.ofSeconds(cacheProperties.snapshotTtlSeconds());
    }

    @Override
    public Cotizacion obtenerCotizacion(String symbol) {
        if (!catalogoPort.validarSimbolo(symbol)) {
            throw new co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException(symbol);
        }

        return cachePort.get(symbol)
                .orElseGet(() -> {
                    Cotizacion cotizacion = alpacaPort.obtenerCotizacion(symbol);
                    cachePort.put(symbol, cotizacion, quoteTtl);
                    return cotizacion;
                });
    }

    @Override
    public List<Cotizacion> obtenerCotizaciones(List<String> symbols) {
        for (String symbol : symbols) {
            if (!catalogoPort.validarSimbolo(symbol)) {
                throw new co.edu.udistrital.acciones.m4datosmercado.application.exception.SimboloInvalidoException(symbol);
            }
        }

        List<String> cacheMiss = symbols.stream()
                .filter(s -> cachePort.get(s).isEmpty())
                .toList();

        if (!cacheMiss.isEmpty()) {
            List<Cotizacion> fresh = alpacaPort.obtenerCotizaciones(cacheMiss);
            for (Cotizacion cotizacion : fresh) {
                cachePort.put(cotizacion.symbol(), cotizacion, quoteTtl);
            }
        }

        return symbols.stream()
                .map(s -> cachePort.get(s).orElseThrow())
                .toList();
    }
}