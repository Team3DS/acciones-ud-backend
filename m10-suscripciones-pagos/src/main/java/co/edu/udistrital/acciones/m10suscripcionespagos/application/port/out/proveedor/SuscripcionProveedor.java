package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor;

import java.time.Instant;

public record SuscripcionProveedor(
        String id,
        String estado,
        boolean cancelarAlFinal,
        Instant canceladaEn,
        Instant finalizadaEn,
        String motivoCancelacion,
        Long userIdMetadata,
        String customerId,
        String lookupKey,
        Instant periodoInicio,
        Instant periodoFin) {
}
