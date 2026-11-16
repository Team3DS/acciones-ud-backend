package co.edu.udistrital.acciones.m3catalogomercados.application.command;

import co.edu.udistrital.acciones.m3catalogomercados.domain.HorarioReferencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearMercadoCommand(
        @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9]{4}") String mic,
        @com.fasterxml.jackson.annotation.JsonAlias("timezone") @NotBlank @Size(max = 64)
                String timeZone,
        @Pattern(regexp = "USD") String currency,
        @Size(max = 50) String country,
        HorarioReferencia tradingHours) {}
