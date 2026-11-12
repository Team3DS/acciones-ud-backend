package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiberarReservaRequest(
        @NotBlank @Size(max = 255) String reason) {
}
