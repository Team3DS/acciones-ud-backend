package co.edu.udistrital.acciones.m6portafolio.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.OutboxEvento;
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
            WHERE publicado = FALSE
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
    public OutboxEvento guardar(OutboxEvento evento) {
        return repositorio.save(evento);
    }

    @Override
    public List<OutboxEvento> buscarPendientes(int lote) {
        return repositorio.buscarPendientes(lote);
    }
}
