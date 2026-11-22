package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.rest;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.ClientesHttpProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CacheCotizaciones {

    private final ConcurrentHashMap<String, Entrada> cotizaciones = new ConcurrentHashMap<>();
    private final ClientesHttpProperties propiedades;
    private final Clock clock;

    public CacheCotizaciones(ClientesHttpProperties propiedades, Clock clock) {
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public Optional<Cotizacion> obtener(String symbol) {
        Entrada entrada = cotizaciones.get(symbol);
        if (entrada == null) {
            return Optional.empty();
        }
        if (!Instant.now(clock).isBefore(entrada.creadoEn().plusSeconds(propiedades.cacheTtlSeconds()))) {
            cotizaciones.remove(symbol, entrada);
            return Optional.empty();
        }
        return Optional.of(entrada.cotizacion());
    }

    public void guardar(Cotizacion cotizacion) {
        cotizaciones.put(cotizacion.symbol(), new Entrada(cotizacion, Instant.now(clock)));
    }

    private record Entrada(Cotizacion cotizacion, Instant creadoEn) {
    }
}
