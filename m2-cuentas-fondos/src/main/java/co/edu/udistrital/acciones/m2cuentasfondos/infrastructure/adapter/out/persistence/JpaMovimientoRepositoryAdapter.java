package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataMovimientoRepository extends JpaRepository<Movimiento, Long>,
        JpaSpecificationExecutor<Movimiento> {

    Optional<Movimiento> findByIdempotencyKey(String idempotencyKey);
}

@Repository
public class JpaMovimientoRepositoryAdapter implements MovimientoRepositoryPort {

    private final SpringDataMovimientoRepository repositorio;

    public JpaMovimientoRepositoryAdapter(SpringDataMovimientoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Movimiento save(Movimiento movimiento) {
        return repositorio.save(movimiento);
    }

    @Override
    public Optional<Movimiento> findByIdempotencyKey(String idempotencyKey) {
        return repositorio.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public Page<Movimiento> buscar(Long cuentaId, FiltroMovimientos filtro, Pageable pageable) {
        return repositorio.findAll(MovimientoSpecifications.filtros(cuentaId, filtro), pageable);
    }
}
