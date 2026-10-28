package com.gfc.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CrearComidaRequest(
        String nombre,
        BigDecimal precio,
        String imagenURL,
        List<ConfigIngredienteRequest> ingredientes
) {
    public CrearComidaRequest {
        if (ingredientes == null) {
            ingredientes = List.of();
        }
    }

    public record ConfigIngredienteRequest(
            Long ingredienteId,
            int cantidadBase,
            boolean incluidoPorDefecto,
            boolean permiteAdicional,
            boolean permiteRetirar,
            BigDecimal precioAdicional
    ) {
    }
}
