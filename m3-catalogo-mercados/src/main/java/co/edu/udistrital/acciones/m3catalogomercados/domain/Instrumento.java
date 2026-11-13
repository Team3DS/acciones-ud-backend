package co.edu.udistrital.acciones.m3catalogomercados.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Instrumento(
        long id,
        String symbol,
        String companyName,
        boolean enabled,
        Mercado market,
        Sector sector,
        TipoInstrumento type,
        BigDecimal lotSize,
        BigDecimal tickSize,
        EstadoInstrumento state,
        Instant createdAt,
        Instant updatedAt) {
    public Instrumento(
            long id,
            String symbol,
            String companyName,
            boolean enabled,
            Mercado market,
            Sector sector) {
        this(
                id,
                symbol,
                companyName,
                enabled,
                market,
                sector,
                TipoInstrumento.ACCION,
                BigDecimal.ONE,
                new BigDecimal("0.01"),
                enabled ? EstadoInstrumento.ACTIVO : EstadoInstrumento.SUSPENDIDO,
                null,
                null);
    }

    public boolean tradable() {
        return enabled && market.enabled() && state == EstadoInstrumento.ACTIVO;
    }
}
