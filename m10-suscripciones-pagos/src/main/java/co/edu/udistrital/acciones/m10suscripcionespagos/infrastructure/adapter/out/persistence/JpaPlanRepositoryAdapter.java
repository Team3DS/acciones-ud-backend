package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface SpringDataPlanRepository extends JpaRepository<Plan, Long> {

    @EntityGraph(attributePaths = "precios")
    @Query("select distinct p from Plan p where p.activo = true order by p.codigo asc")
    List<Plan> findActivosConPrecios();

    @Query("select p from PrecioPlan p join fetch p.plan where p.stripeLookupKey = :lookupKey")
    Optional<PrecioPlan> findByStripeLookupKey(@Param("lookupKey") String lookupKey);

    @Query("select p from PrecioPlan p join fetch p.plan where p.id = :id")
    Optional<PrecioPlan> findPrecioConPlan(@Param("id") Long id);

    @Query("""
            select p from PrecioPlan p join fetch p.plan pl
            where pl.codigo = :codigo and p.intervalo = :intervalo and p.activo = true
            """)
    Optional<PrecioPlan> findActivoPorPlanEIntervalo(@Param("codigo") String codigo,
                                                     @Param("intervalo") IntervaloPlan intervalo);
}

@Repository
public class JpaPlanRepositoryAdapter implements PlanRepositoryPort {

    private final SpringDataPlanRepository repositorio;

    public JpaPlanRepositoryAdapter(SpringDataPlanRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public List<Plan> findActivos() {
        return repositorio.findActivosConPrecios();
    }

    @Override
    public Optional<PrecioPlan> findPrecioPorLookupKey(String lookupKey) {
        return repositorio.findByStripeLookupKey(lookupKey);
    }

    @Override
    public Optional<PrecioPlan> findPrecioPorId(Long id) {
        return repositorio.findPrecioConPlan(id);
    }

    @Override
    public Optional<PrecioPlan> findPrecioActivo(String codigoPlan, IntervaloPlan intervalo) {
        return repositorio.findActivoPorPlanEIntervalo(codigoPlan, intervalo);
    }
}
