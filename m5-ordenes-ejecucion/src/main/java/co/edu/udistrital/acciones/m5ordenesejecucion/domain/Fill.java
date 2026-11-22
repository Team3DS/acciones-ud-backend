package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record Fill(
        String fillId,
        BigDecimal precio,
        Integer cantidad,
        BigDecimal comision,
        Instant ejecutadaEn
) {}
