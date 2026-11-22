package co.edu.udistrital.acciones.m10suscripcionespagos.application.service;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CatalogoPreciosProveedor {

    private final PaymentProviderPort proveedor;
    private final StripeProperties propiedades;
    private final Clock clock;
    private final Map<String, Entrada> cache = new ConcurrentHashMap<>();

    public CatalogoPreciosProveedor(PaymentProviderPort proveedor,
                                    StripeProperties propiedades,
                                    Clock clock) {
        this.proveedor = proveedor;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    public PrecioProveedor obtener(String lookupKey) {
        Instant ahora = clock.instant();
        Entrada entrada = cache.get(lookupKey);
        if (entrada != null && entrada.expiraEn().isAfter(ahora)) {
            return entrada.precio();
        }
        PrecioProveedor precio = proveedor.precioPorLookupKey(lookupKey);
        cache.put(lookupKey, new Entrada(precio, ahora.plus(propiedades.preciosCacheTtl())));
        return precio;
    }

    private record Entrada(PrecioProveedor precio, Instant expiraEn) {
    }
}
