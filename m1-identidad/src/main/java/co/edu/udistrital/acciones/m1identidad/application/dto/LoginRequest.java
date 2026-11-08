package co.edu.udistrital.acciones.m1identidad.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@Email @NotBlank String correo, @NotBlank String password) {}
