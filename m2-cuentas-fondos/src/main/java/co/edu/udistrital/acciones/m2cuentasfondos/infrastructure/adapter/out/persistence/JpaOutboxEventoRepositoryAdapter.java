package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SpringDataOutboxEventoRepository extends JpaRepository<OutboxEvento, UUID> {

    @Query(value = """
            SELECT * FROM outbox_evento
            WHERE publicado = false
            ORDER BY creado_en ASC
            LIMIT :lote
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvento> buscarPendientes(@Param("lote") int lote);
}

@Repository
public class JpaOutboxEventoRepositoryAdapter implements OutboxEventoRepositoryPort {

    private final SpringDataOutboxEventoRepository repositorio;

    public JpaOutboxEventoRepositoryAdapter(SpringDataOutboxEventoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public OutboxEvento save(OutboxEvento evento) {
        return repositorio.save(evento);
    }

    @Override
    public List<OutboxEvento> buscarPendientes(int lote) {
        return repositorio.buscarPendientes(lote);
    }
}
