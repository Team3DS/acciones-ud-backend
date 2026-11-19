package co.edu.udistrital.acciones.m3catalogomercados.application.command;

import co.edu.udistrital.acciones.m3catalogomercados.domain.HorarioReferencia;

import jakarta.validation.constraints.Size;

public record ActualizarMercadoCommand(
        @Size(min = 1, max = 100) String name,
        Boolean enabled,
        @Size(max = 50) String country,
        HorarioReferencia tradingHours) {}
