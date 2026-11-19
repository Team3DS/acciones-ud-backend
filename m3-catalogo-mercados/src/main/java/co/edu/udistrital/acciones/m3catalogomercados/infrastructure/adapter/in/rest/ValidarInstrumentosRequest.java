package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ValidarInstrumentosRequest(
        @NotEmpty @Size(max = 100) List<@NotBlank @Size(max = 20) String> symbols,
        @Size(max = 30) String market) {}
