package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;

import java.util.List;
import java.util.Optional;

public interface PlanRepositoryPort {

    List<Plan> findActivos();

    Optional<PrecioPlan> findPrecioPorLookupKey(String lookupKey);

    Optional<PrecioPlan> findPrecioPorId(Long id);

    Optional<PrecioPlan> findPrecioActivo(String codigoPlan, IntervaloPlan intervalo);
}
