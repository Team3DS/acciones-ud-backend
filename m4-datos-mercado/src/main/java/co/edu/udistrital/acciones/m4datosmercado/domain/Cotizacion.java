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
    BigDecimal prevClose
) {}
