package co.edu.udistrital.acciones.m4datosmercado.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.util.List;

public record MercadoActualizadoPayload(
        @JsonProperty("mic") String mic,
        @JsonProperty("nombre") String nombre,
        @JsonProperty("pais") String pais,
        @JsonProperty("moneda") String moneda,
        @JsonProperty("zonaHoraria") String zonaHoraria,
        @JsonProperty("horarios") List<MercadoCreadoPayload.HorarioPayload> horarios) {}