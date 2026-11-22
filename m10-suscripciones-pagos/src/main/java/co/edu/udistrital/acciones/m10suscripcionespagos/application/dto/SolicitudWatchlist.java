package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SolicitudWatchlist(@NotBlank String symbol) {
}
