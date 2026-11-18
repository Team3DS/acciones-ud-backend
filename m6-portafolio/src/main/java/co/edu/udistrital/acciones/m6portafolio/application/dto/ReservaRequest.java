package co.edu.udistrital.acciones.m6portafolio.application.dto;

import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReservaRequest(
        @NotBlank @Size(max = 100) String orderId,
        @NotNull @Positive Long userId,
        @NotBlank @Pattern(regexp = Simbolos.PATRON_TEXTO) String symbol,
        @NotNull @Positive @Digits(integer = 11, fraction = 0) BigDecimal quantity,
        @Size(max = 64) String idempotencyKey) {
}
