package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;

import java.math.BigDecimal;
import java.time.Instant;

public record WatchlistItemResponse(
        String symbol,
        Instant addedAt,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePct,
        Instant priceTimestamp,
        boolean stale) {

    public static WatchlistItemResponse desde(WatchlistItem item, Cotizacion cotizacion, boolean stale) {
        if (cotizacion == null) {
            return new WatchlistItemResponse(item.getSymbol(), item.getAgregadoEn(),
                    null, null, null, null, stale);
        }
        return new WatchlistItemResponse(item.getSymbol(), item.getAgregadoEn(),
                cotizacion.price(), cotizacion.change(), cotizacion.changePct(),
                cotizacion.timestamp(), stale);
    }
}
