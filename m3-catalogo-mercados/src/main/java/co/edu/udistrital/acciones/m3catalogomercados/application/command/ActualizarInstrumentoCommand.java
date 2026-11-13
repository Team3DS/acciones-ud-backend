package co.edu.udistrital.acciones.m3catalogomercados.application.command;

import co.edu.udistrital.acciones.m3catalogomercados.domain.EstadoInstrumento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarInstrumentoCommand(
        @Size(min = 1, max = 150) String name,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8)
                BigDecimal lotSize,
        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8)
                BigDecimal tickSize,
        EstadoInstrumento state) {}
