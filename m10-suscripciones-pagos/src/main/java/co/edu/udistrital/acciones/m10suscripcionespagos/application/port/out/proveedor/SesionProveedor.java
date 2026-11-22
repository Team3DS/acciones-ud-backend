package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor;

import java.time.Instant;

public record SesionProveedor(
        String id,
        String url,
        Instant expiraEn,
        String customerId,
        String subscriptionId) {
}
