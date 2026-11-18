package co.edu.udistrital.acciones.m6portafolio.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FillPayload(
        BigDecimal price,
        BigDecimal qty,
        Instant timestamp) {
}
