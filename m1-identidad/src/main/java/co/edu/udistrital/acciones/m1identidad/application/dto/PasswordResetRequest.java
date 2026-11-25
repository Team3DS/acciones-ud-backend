package co.edu.udistrital.acciones.m1identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(@NotBlank @Email @Size(max = 254) String correo) {
    @Override
    public String toString() { return "PasswordResetRequest[redacted]"; }
}
