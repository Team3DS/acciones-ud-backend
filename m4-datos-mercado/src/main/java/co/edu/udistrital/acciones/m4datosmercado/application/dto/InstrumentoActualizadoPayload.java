package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Set;

public record InstrumentoActualizadoPayload(
        @JsonProperty("symbol") String symbol,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("mercadoMic") String mercadoMic,
        @JsonProperty("moneda") String moneda,
        @JsonProperty("decimalesPrecio") int decimalesPrecio,
        @JsonProperty("loteMinimo") long loteMinimo,
        @JsonProperty("sectores") Set<String> sectores,
        @JsonProperty("metadata") String metadata) {}