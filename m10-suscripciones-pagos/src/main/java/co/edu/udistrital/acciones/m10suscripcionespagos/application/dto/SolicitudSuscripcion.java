package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import jakarta.validation.constraints.NotNull;

public record SolicitudSuscripcion(@NotNull IntervaloPlan interval) {
}
