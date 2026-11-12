package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenRechazadaPayload(
        String orderId,
        String reason,
        String brokerError) {
}
