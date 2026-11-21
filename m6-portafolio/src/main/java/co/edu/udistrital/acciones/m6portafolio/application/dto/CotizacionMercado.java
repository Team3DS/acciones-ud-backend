package co.edu.udistrital.acciones.m6portafolio.application.dto;

import co.edu.udistrital.acciones.m6portafolio.domain.Montos;

import java.math.BigDecimal;
import java.time.Instant;

public record CotizacionMercado(String symbol, BigDecimal price, Instant timestamp) {

    public CotizacionMercado {
        price = Montos.validarPositivo(price);
    }
}
