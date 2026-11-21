package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxEventoJpaRepository extends JpaRepository<OutboxEventoEntity, UUID> {
    @Query("SELECT e FROM OutboxEventoEntity e WHERE e.publicadoEn IS NULL ORDER BY e.creadoEn ASC")
    List<OutboxEventoEntity> findPendientes(Pageable pageable);
}
