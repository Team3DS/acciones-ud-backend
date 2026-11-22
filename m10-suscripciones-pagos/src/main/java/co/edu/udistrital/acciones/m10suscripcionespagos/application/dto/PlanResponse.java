package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;

import java.util.List;

public record PlanResponse(
        String code,
        String name,
        String description,
        List<PrecioPlanResponse> prices) {

    public static PlanResponse de(Plan plan) {
        return new PlanResponse(
                plan.getCodigo(),
                plan.getNombre(),
                plan.getDescripcion(),
                plan.preciosActivos().stream().map(PrecioPlanResponse::de).toList());
    }
}
