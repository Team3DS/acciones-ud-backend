package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;

public record ResultadoCheckout(SesionCheckout sesion, boolean reutilizada) {
}
