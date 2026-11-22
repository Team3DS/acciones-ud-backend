package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor;

public record PrecioProveedor(
        String id,
        long montoCentavos,
        String moneda,
        String lookupKey) {
}
