package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.EjecucionReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.EjecucionReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

interface SpringDataEjecucionReservaRepository extends JpaRepository<EjecucionReserva, Long> {
    Optional<EjecucionReserva> findByReservaIdAndExecutionId(Long reservaId, String executionId);
}

@Repository
public class JpaEjecucionReservaRepositoryAdapter implements EjecucionReservaRepositoryPort {
    private final SpringDataEjecucionReservaRepository repositorio;

    public JpaEjecucionReservaRepositoryAdapter(SpringDataEjecucionReservaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Optional<EjecucionReserva> findByReservaIdAndExecutionId(Long reservaId, String executionId) {
        return repositorio.findByReservaIdAndExecutionId(reservaId, executionId);
    }

    @Override
    public EjecucionReserva save(EjecucionReserva ejecucion) {
        return repositorio.save(ejecucion);
    }
}
