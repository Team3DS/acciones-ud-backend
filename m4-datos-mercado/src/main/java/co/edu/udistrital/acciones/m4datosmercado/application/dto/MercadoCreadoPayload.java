package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.util.List;

public record MercadoCreadoPayload(
        @JsonProperty("mic") String mic,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("pais") String pais,
        @JsonProperty("moneda") String moneda,
        @JsonProperty("zonaHoraria") String zonaHoraria,
        @JsonProperty("horarios") List<HorarioPayload> horarios) {

    public record HorarioPayload(
            @JsonProperty("diaSemana") int diaSemana,
            @JsonProperty("apertura") LocalTime apertura,
            @JsonProperty("cierre") LocalTime cierre,
            @JsonProperty("preMarket") LocalTime preMarket,
            @JsonProperty("afterHours") LocalTime afterHours) {}
}