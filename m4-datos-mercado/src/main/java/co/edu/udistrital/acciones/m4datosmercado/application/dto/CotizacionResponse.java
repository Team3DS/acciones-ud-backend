package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import co.edu.udistrital.acciones.m4datosmercado.domain.Cotizacion;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record CotizacionResponse(
        String symbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePct,
        Long volume,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        BigDecimal prevClose) {

    public static CotizacionResponse from(Cotizacion cotizacion) {
        return new CotizacionResponse(
                cotizacion.symbol(),
                cotizacion.price(),
                cotizacion.change(),
                cotizacion.changePct(),
                cotizacion.volume(),
                cotizacion.timestamp(),
                cotizacion.bid(),
                cotizacion.ask(),
                cotizacion.high(),
                cotizacion.low(),
                cotizacion.open(),
                cotizacion.prevClose()
        );
    }
}