package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface SpringDataMovimientoPosicionRepository extends JpaRepository<MovimientoPosicion, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);
}

@Repository
public class JpaMovimientoPosicionRepositoryAdapter implements MovimientoPosicionRepositoryPort {

    private final SpringDataMovimientoPosicionRepository repositorio;

    public JpaMovimientoPosicionRepositoryAdapter(SpringDataMovimientoPosicionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public MovimientoPosicion save(MovimientoPosicion movimiento) {
        return repositorio.save(movimiento);
    }

    @Override
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        return repositorio.existsByIdempotencyKey(idempotencyKey);
    }
}
