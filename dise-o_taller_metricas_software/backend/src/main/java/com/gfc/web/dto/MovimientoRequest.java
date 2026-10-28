package com.gfc.web.dto;

import java.time.LocalDateTime;

/** fecha es opcional (se usa la actual si no viene); motivo es obligatorio en ambos casos. */
public record MovimientoRequest(int cantidad, String motivo, LocalDateTime fecha) {
}
