package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataMovimientoRepository extends JpaRepository<Movimiento, Long> {

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
}
