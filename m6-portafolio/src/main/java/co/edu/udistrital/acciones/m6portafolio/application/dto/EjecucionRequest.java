package co.edu.udistrital.acciones.m6portafolio.application.dto;

import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record EjecucionRequest(
        @NotBlank @Size(max = 100) String orderId,
        @NotBlank @Size(max = 100) String executionId,
        @NotNull @Positive Long userId,
        @NotBlank @Pattern(regexp = Simbolos.PATRON_TEXTO) String symbol,
        @NotNull LadoEjecucion side,
        @NotNull @Positive @Digits(integer = 11, fraction = 0) BigDecimal quantity,
        @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal price,
        Instant executedAt,
        String eventId) {
}
