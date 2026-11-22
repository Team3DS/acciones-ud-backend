package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor;

import java.time.Instant;

public record ReciboProveedor(
        String id,
        String numero,
        long totalCentavos,
        String moneda,
        String estado,
        Instant periodoInicio,
        Instant periodoFin,
        Instant emitidoEn,
        Instant pagadoEn,
        long intento,
        String urlRecibo,
        String urlPdf,
        String stripeSubscriptionId) {
}
