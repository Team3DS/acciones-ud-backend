package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface OutboxPendienteDao extends JpaRepository<OutboxEvento, UUID> {

    @Query(value = """
            SELECT ev.* FROM outbox_evento ev
            WHERE ev.publicado = FALSE
            ORDER BY ev.creado_en ASC
            LIMIT :lote
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvento> leerPendientes(@Param("lote") int lote);
}

@Repository
public class JpaOutboxEventoRepositoryAdapter implements OutboxEventoRepositoryPort {

    private final OutboxPendienteDao dao;

    public JpaOutboxEventoRepositoryAdapter(OutboxPendienteDao dao) {
        this.dao = dao;
    }

    @Override
    public List<OutboxEvento> buscarPendientes(int lote) {
        return dao.leerPendientes(lote);
    }

    @Override
    public OutboxEvento guardar(OutboxEvento evento) {
        return dao.save(evento);
    }
}
