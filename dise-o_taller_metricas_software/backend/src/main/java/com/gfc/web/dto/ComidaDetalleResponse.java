package com.gfc.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComidaDetalleResponse(Long id, String nombre, BigDecimal precio, String imagenURL,
                                     List<ConfiguracionResponse> ingredientes) {

    public record ConfiguracionResponse(Long id, IngredienteResumen ingrediente, int cantidadBase,
                                         boolean incluidoPorDefecto, boolean permiteAdicional,
                                         boolean permiteRetirar, BigDecimal precioAdicional) {
    }

    public record IngredienteResumen(Long id, String nombreIngrediente, String unidadMedida) {
    }
}
