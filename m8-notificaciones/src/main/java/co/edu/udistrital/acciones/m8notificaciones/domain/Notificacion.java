package co.edu.udistrital.acciones.m8notificaciones.domain;

import java.time.Instant;

public record Notificacion(
        long id,
        long userId,
        TipoAviso type,
        String title,
        String message,
        Long orderId,
        String market,
        Instant createdAt,
        Instant readAt) {}
