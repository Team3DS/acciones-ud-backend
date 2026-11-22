package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoRecibo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Recibo;

import java.math.BigDecimal;
import java.time.Instant;

public record ReciboResponse(
        Long id,
        String number,
        BigDecimal amount,
        String currency,
        EstadoRecibo status,
        Instant periodStart,
        Instant periodEnd,
        Instant issuedAt,
        Instant paidAt,
        int attempt,
        String receiptUrl,
        String pdfUrl) {

    public static ReciboResponse de(Recibo recibo) {
        return new ReciboResponse(
                recibo.getId(),
                recibo.getNumero(),
                recibo.getMonto(),
                recibo.getMoneda().strip(),
                recibo.getEstado(),
                recibo.getPeriodoInicio(),
                recibo.getPeriodoFin(),
                recibo.getEmitidoEn(),
                recibo.getPagadoEn(),
                recibo.getIntento(),
                recibo.getUrlRecibo(),
                recibo.getUrlPdf());
    }
}
