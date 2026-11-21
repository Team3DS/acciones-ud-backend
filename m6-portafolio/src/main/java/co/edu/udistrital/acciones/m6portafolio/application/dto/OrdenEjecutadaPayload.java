package co.edu.udistrital.acciones.m6portafolio.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenEjecutadaPayload(
        String orderId,
        String executionId,
        List<FillPayload> fills,
        BigDecimal totalAmount,
        Long userId,
        String symbol,
        String side) {

    public boolean traeDatosDePosicion() {
        return userId != null && texto(symbol) && texto(side);
    }

    private static boolean texto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
