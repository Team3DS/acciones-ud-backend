package com.gfc.web.dto;

import java.time.LocalDateTime;

public record MovimientoResultadoResponse(Movimiento movimiento, int stockActual) {

    public record Movimiento(Long id, String tipo, int cantidad, LocalDateTime fecha, boolean aplicado) {
    }
}
