package co.edu.udistrital.acciones.m3catalogomercados.application.command;

import co.edu.udistrital.acciones.m3catalogomercados.domain.TipoInstrumento;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearInstrumentoCommand(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9.\\-]{0,19}") String symbol,
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 30) String market,
        @NotBlank @Size(max = 30) String sector,
        @NotNull TipoInstrumento type,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8)
                BigDecimal lotSize,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8)
                BigDecimal tickSize) {}
