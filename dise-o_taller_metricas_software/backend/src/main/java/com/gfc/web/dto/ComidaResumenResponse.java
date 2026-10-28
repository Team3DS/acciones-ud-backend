package com.gfc.web.dto;

import java.math.BigDecimal;

public record ComidaResumenResponse(Long id, String nombre, BigDecimal precio, String imagenURL) {
}
