package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConfirmacionEjecucionJpaRepository extends JpaRepository<ConfirmacionEjecucionEntity, ConfirmacionEjecucionId> {
    @Query("SELECT c FROM ConfirmacionEjecucionEntity c WHERE c.id.ejecucionOrdenId = :ejecucionId")
    List<ConfirmacionEjecucionEntity> findByEjecucionOrdenId(@Param("ejecucionId") Long ejecucionId);
}
