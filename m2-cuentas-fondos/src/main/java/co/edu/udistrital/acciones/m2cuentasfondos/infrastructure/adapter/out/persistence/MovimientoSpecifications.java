package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class MovimientoSpecifications {

    private MovimientoSpecifications() {
    }

    public static Specification<Movimiento> filtros(Long cuentaId, FiltroMovimientos filtro) {
        return (raiz, consulta, criterios) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(criterios.equal(raiz.get("cuentaId"), cuentaId));
            if (filtro.tipo() != null) {
                predicados.add(criterios.equal(raiz.get("tipo"), filtro.tipo()));
            }
            if (filtro.desde() != null) {
                predicados.add(criterios.greaterThanOrEqualTo(raiz.get("creadoEn"), filtro.desde()));
            }
            if (filtro.hasta() != null) {
                predicados.add(criterios.lessThanOrEqualTo(raiz.get("creadoEn"), filtro.hasta()));
            }
            return criterios.and(predicados.toArray(Predicate[]::new));
        };
    }
}
