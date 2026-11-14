package co.edu.udistrital.acciones.m4datosmercado.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Cotizacion(
        String symbol,
        BigDecimal price,
        BigDecimal change,
        BigDecimal changePct,
        Long volume,
        Instant timestamp,
        BigDecimal bid,
        BigDecimal ask,
        BigDecimal high,
        BigDecimal low,
        BigDecimal open,
        BigDecimal prevClose) {

    public Cotizacion {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("El símbolo no puede ser nulo o vacío");
        }
        if (price == null) {
            throw new IllegalArgumentException("El precio no puede ser nulo");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("El timestamp no puede ser nulo");
        }
    }

    public static Cotizacion basica(String symbol, BigDecimal price, Instant timestamp) {
        return new Cotizacion(symbol, price, BigDecimal.ZERO, BigDecimal.ZERO, 0L, timestamp,
                null, null, null, null, null, null);
    }
}