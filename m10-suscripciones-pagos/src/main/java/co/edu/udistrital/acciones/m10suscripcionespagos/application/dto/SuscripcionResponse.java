package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;

import java.math.BigDecimal;
import java.time.Instant;

public record SuscripcionResponse(
        Long id,
        String plan,
        IntervaloPlan interval,
        BigDecimal amount,
        String currency,
        EstadoSuscripcion status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant canceledAt,
        Instant endedAt) {

    public static SuscripcionResponse de(Suscripcion suscripcion) {
        PrecioPlan precio = suscripcion.getPrecioPlan();
        return new SuscripcionResponse(
                suscripcion.getId(),
                precio.getPlan().getNombre(),
                precio.getIntervalo(),
                precio.getMonto(),
                precio.getMoneda().strip(),
                suscripcion.getEstado(),
                suscripcion.getPeriodoInicio(),
                suscripcion.getPeriodoFin(),
                suscripcion.isCancelarAlFinal(),
                suscripcion.getCanceladaEn(),
                suscripcion.getFinalizadaEn());
    }
}
