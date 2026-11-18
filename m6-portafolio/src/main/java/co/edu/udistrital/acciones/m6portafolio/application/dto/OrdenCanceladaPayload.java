package co.edu.udistrital.acciones.m6portafolio.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrdenCanceladaPayload(
        String orderId,
        String reason) {
}
