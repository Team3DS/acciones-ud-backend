package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.mercado;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.M4ClientProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheCotizaciones {

    private final ConcurrentHashMap<String, Entrada> cotizaciones = new ConcurrentHashMap<>();
    private final M4ClientProperties propiedades;
    private final Clock clock;

    public CacheCotizaciones(M4ClientProperties propiedades, Clock clock) {
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public Optional<CotizacionMercado> obtener(String symbol) {
        Entrada entrada = cotizaciones.get(symbol);
        if (entrada == null || !Instant.now(clock).isBefore(entrada.expiraEn())) {
            cotizaciones.remove(symbol, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.cotizacion());
    }

    public void guardar(CotizacionMercado cotizacion) {
        cotizaciones.put(cotizacion.symbol(), new Entrada(cotizacion,
                Instant.now(clock).plusSeconds(propiedades.cacheTtlSeconds())));
    }

    public void limpiar() {
        cotizaciones.clear();
    }

    private record Entrada(CotizacionMercado cotizacion, Instant expiraEn) {
    }
}
