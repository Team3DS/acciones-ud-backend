package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.ReciboProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SolicitudCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;

public interface PaymentProviderPort {

    SesionProveedor crearSesionCheckout(SolicitudCheckout solicitud);

    void expirarSesion(String stripeSessionId);

    SesionProveedor obtenerSesion(String stripeSessionId);

    SuscripcionProveedor obtenerSuscripcion(String stripeSubscriptionId);

    SuscripcionProveedor programarCancelacion(String stripeSubscriptionId, String idempotencyKey);

    SuscripcionProveedor cambiarPrecio(String stripeSubscriptionId, String itemId,
                                       String nuevoPrecioId, String idempotencyKey);

    ReciboProveedor obtenerRecibo(String stripeInvoiceId);

    PrecioProveedor precioPorLookupKey(String lookupKey);
}
