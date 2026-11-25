package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ConfirmarReservaRequest(
        @NotBlank @Size(max = 100) String executionId,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        Boolean finalExecution,
        @Min(1) Integer expectedExecutionCount) {

    public ConfirmarReservaRequest(String executionId, BigDecimal amount) {
        this(executionId, amount, true, null);
    }

    public ConfirmarReservaRequest(String executionId, BigDecimal amount, Boolean finalExecution) {
        this(executionId, amount, finalExecution, null);
    }

    public boolean esFinal() {
        return !Boolean.FALSE.equals(finalExecution);
    }
}
