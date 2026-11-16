package co.edu.udistrital.acciones.m8notificaciones.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AlertaPrecio(
        long id,
        long userId,
        long instrumentId,
        String symbol,
        CondicionPrecio condition,
        BigDecimal targetPrice,
        boolean active,
        Instant createdAt,
        Instant triggeredAt) {}
