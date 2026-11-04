package co.edu.udistrital.acciones.m1identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(
    @NotBlank String nombres,
    @NotBlank String apellidos,
    @NotBlank String tipoDocumento,
    @NotBlank String numeroDocumento,
    @Email @NotBlank String correo,
    @NotBlank String password,
    @NotBlank String nivelInversionista
) {}
