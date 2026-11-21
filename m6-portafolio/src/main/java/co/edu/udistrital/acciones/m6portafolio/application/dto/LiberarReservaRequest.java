package co.edu.udistrital.acciones.m6portafolio.application.dto;

import jakarta.validation.constraints.Size;

public record LiberarReservaRequest(@Size(max = 255) String reason) {
}
