package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import co.edu.udistrital.acciones.m4datosmercado.domain.HistoricoCotizacion;
import co.edu.udistrital.acciones.m4datosmercado.domain.Intervalo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoricoCotizacionResponse(
        Long id,
        String symbol,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
        String intervalo) {

    public static HistoricoCotizacionResponse from(HistoricoCotizacion historico) {
        return new HistoricoCotizacionResponse(
                historico.getId(),
                historico.getSymbol(),
                historico.getOpen(),
                historico.getHigh(),
                historico.getLow(),
                historico.getClose(),
                historico.getVolume(),
                historico.getTimestamp(),
                historico.getIntervalo().getValor()
        );
    }
}