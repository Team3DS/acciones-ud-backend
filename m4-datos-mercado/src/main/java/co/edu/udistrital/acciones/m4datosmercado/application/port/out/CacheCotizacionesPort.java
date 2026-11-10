package co.edu.udistrital.acciones.m4datosmercado.application.port.out;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import java.time.Duration;
import java.util.Optional;

public interface CacheCotizacionesPort {
    Optional<Cotizacion> get(String symbol);
    void put(String symbol, Cotizacion cotizacion, Duration ttl);
    void evict(String symbol);
}
