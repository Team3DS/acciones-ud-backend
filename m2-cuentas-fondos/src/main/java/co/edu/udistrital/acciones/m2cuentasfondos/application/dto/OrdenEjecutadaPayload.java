package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenEjecutadaPayload(
        String orderId,
        String executionId,
        List<JsonNode> fills,
        BigDecimal totalAmount,
        String side,
        Long userId) {

    public boolean esVentaDelUsuario() {
        return "SELL".equalsIgnoreCase(side) && userId != null;
    }
}
