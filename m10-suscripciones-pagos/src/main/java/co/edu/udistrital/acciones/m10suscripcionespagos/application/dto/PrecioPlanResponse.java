package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;

import java.math.BigDecimal;

public record PrecioPlanResponse(
        IntervaloPlan interval,
        BigDecimal amount,
        String currency) {

    public static PrecioPlanResponse de(PrecioPlan precio) {
        return new PrecioPlanResponse(precio.getIntervalo(), precio.getMonto(), precio.getMoneda().strip());
    }
}
