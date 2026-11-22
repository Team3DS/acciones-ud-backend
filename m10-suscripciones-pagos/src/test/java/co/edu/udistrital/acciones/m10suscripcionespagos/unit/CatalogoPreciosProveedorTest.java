package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.CatalogoPreciosProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogoPreciosProveedorTest {

    private static final String LOOKUP = "acciones_ud_premium_mensual";
    private static final Instant BASE = Instant.parse("2026-03-01T00:00:00Z");

    private final PaymentProviderPort proveedor = mock(PaymentProviderPort.class);
    private final StripeProperties propiedades = new StripeProperties(
            "sk_test_clave", "whsec_secreto", null, null, null, 2, null, null,
            Duration.ofMinutes(10), null, null);

    @Test
    void laSegundaConsultaDentroDelTtlUsaLaCache() {
        Clock clock = Clock.fixed(BASE, ZoneOffset.UTC);
        CatalogoPreciosProveedor catalogo = new CatalogoPreciosProveedor(proveedor, propiedades, clock);
        when(proveedor.precioPorLookupKey(LOOKUP))
                .thenReturn(new PrecioProveedor("price_1", 1200L, "usd", LOOKUP));

        catalogo.obtener(LOOKUP);
        PrecioProveedor segunda = catalogo.obtener(LOOKUP);

        assertThat(segunda.id()).isEqualTo("price_1");
        verify(proveedor, times(1)).precioPorLookupKey(LOOKUP);
    }

    @Test
    void despuesDelTtlVuelveAConsultarAStripe() {
        Clock clock = mock(Clock.class);
        when(clock.instant())
                .thenReturn(BASE)
                .thenReturn(BASE.plus(Duration.ofMinutes(11)));
        CatalogoPreciosProveedor catalogo = new CatalogoPreciosProveedor(proveedor, propiedades, clock);
        when(proveedor.precioPorLookupKey(LOOKUP))
                .thenReturn(new PrecioProveedor("price_1", 1200L, "usd", LOOKUP));

        catalogo.obtener(LOOKUP);
        catalogo.obtener(LOOKUP);

        verify(proveedor, times(2)).precioPorLookupKey(LOOKUP);
    }
}
